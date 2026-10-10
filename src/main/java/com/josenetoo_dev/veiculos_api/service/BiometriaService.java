package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.verificacao.SessaoBiometriaResponse;
import com.josenetoo_dev.veiculos_api.enums.*;
import com.josenetoo_dev.veiculos_api.exception.ex.*;
import com.josenetoo_dev.veiculos_api.model.*;
import com.josenetoo_dev.veiculos_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Biometria é um auxílio técnico à revisão humana, não comprovação oficial de identidade.
 * Nenhuma pontuação ou mídia facial é persistida. Sem opt-in, AWS jamais é chamada.
 */
@Service
@RequiredArgsConstructor
public class BiometriaService {
    private static final int MAX_SESSOES_24H = 3;
    private final VerificacaoRepository verificacoes;
    private final EvidenciaVerificacaoRepository evidencias;
    private final SessaoBiometriaRepository sessoes;
    private final UsuarioRepository usuarios;
    private final PrivateEvidenceStorage storage;
    private final BiometricProvider provider;

    @Value("${verification.biometric.enabled:false}")
    private boolean enabled;

    @Value("${verification.biometric.liveness-threshold:90}")
    private float livenessThreshold;

    @Value("${verification.biometric.face-similarity-threshold:90}")
    private float faceThreshold;

    @Value("${verification.biometric.policy-version:biometria-v1}")
    private String policyVersion;

    private static LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }

    public boolean enabled() { return enabled; }

    private Usuario actor() {
        Authentication auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new CredenciaisInvalidasException("Usuário não autenticado");
        }
        try {
            Usuario user=usuarios.findById(Long.valueOf(auth.getName()))
                .orElseThrow(()->new CredenciaisInvalidasException("Usuário não encontrado"));
            if (user.getStatus()!=StatusUsuario.ACTIVE)
                throw new AcessoNegadoException("Confirme seu e-mail antes de iniciar biometria");
            return user;
        } catch(NumberFormatException ex) {
            throw new CredenciaisInvalidasException("Usuário não autenticado");
        }
    }

    private Verificacao identidadeDoDono(Long verificationId, Usuario user) {
        Verificacao item=verificacoes.findByIdForUpdate(verificationId)
            .orElseThrow(VerificacaoNaoEncontradaException::new);
        if (item.getTipo()!=TipoVerificacao.IDENTIDADE
                || !item.getSolicitante().getId().equals(user.getId())) {
            // Evita confirmar a existência de verificações de terceiros.
            throw new VerificacaoNaoEncontradaException();
        }
        return item;
    }

    private EvidenciaVerificacao fotoFrente(Long verificationId) {
        return evidencias.findByVerificacaoId(verificationId).stream()
            .filter(x->x.getTipo()==TipoEvidencia.IDENTIDADE_FRENTE)
            .findFirst().orElseThrow(()->
                new VerificacaoIndisponivelException("Envie a frente do documento antes da biometria"));
    }

    private void requireConfigured() {
        if(!enabled) throw new BiometriaIndisponivelException();
        if (!storage.available()) throw new BiometriaIndisponivelException();
        if(livenessThreshold<1 || livenessThreshold>100
                || faceThreshold<1 || faceThreshold>100
                || policyVersion==null || policyVersion.isBlank() || policyVersion.length()>60) {
            throw new BiometriaIndisponivelException();
        }
    }

    /**
     * Limite por conta/documento, serializado por lock da verificação.
     * Cada sessão tem UUID externo AWS; o cliente nunca escolhe seu ID.
     */
    @Transactional
    public SessaoBiometriaResponse iniciar(Long verificationId) {
        requireConfigured();
        Usuario user=actor();
        Verificacao item=identidadeDoDono(verificationId,user);
        if(item.getStatus()!=StatusVerificacao.RASCUNHO && item.getStatus()!=StatusVerificacao.REJEITADA) {
            throw new VerificacaoIndisponivelException("Envio documental não está em edição");
        }
        EvidenciaVerificacao documento=fotoFrente(verificationId);
        LocalDateTime agora=now();
        if(sessoes.countByVerificacaoIdAndCriadoEmAfter(verificationId,agora.minusDays(1))>=MAX_SESSOES_24H) {
            throw new BiometriaLimiteExcedidoException();
        }
        var anterior=sessoes.findFirstByVerificacaoIdOrderByCriadoEmDesc(verificationId);
        if(anterior.isPresent() && anterior.get().getCriadoEm().isAfter(agora.minusMinutes(1))) {
            throw new BiometriaLimiteExcedidoException();
        }
        String id=provider.criarSessao();
        try { UUID.fromString(id); }
        catch (RuntimeException ex) { throw new BiometriaIndisponivelException(); }
        SessaoBiometria sessao=new SessaoBiometria();
        sessao.setId(id);
        sessao.setVerificacao(item);
        sessao.setDocumentoEvidenciaId(documento.getId());
        sessao.setVersaoPolitica(policyVersion);
        return SessaoBiometriaResponse.from(sessoes.saveAndFlush(sessao));
    }

    /**
     * Consulta exclusivamente no provedor, nunca aceita score/status do navegador.
     * AWS mantém os dados faciais da sessão por período limitado.
     */
    @Transactional
    public SessaoBiometriaResponse consultarResultado(Long verificationId, String sessionId) {
        requireConfigured();
        Usuario user=actor();
        Verificacao item=identidadeDoDono(verificationId,user);
        SessaoBiometria sessao=sessoes.findByIdAndVerificacaoId(sessionId,verificationId)
            .orElseThrow(VerificacaoNaoEncontradaException::new);
        if(sessao.getStatus()!=StatusSessaoBiometria.CRIADA) return SessaoBiometriaResponse.from(sessao);
        // A própria AWS expira o identificador em poucos minutos; nunca reutilizá-lo.
        if(sessao.getCriadoEm().isBefore(now().minusMinutes(3))) {
            concluir(sessao,StatusSessaoBiometria.INCONCLUSIVA);
            return SessaoBiometriaResponse.from(sessao);
        }
        if(item.getStatus()!=StatusVerificacao.RASCUNHO && item.getStatus()!=StatusVerificacao.REJEITADA) {
            throw new VerificacaoIndisponivelException("Verificação já enviada");
        }
        EvidenciaVerificacao doc=fotoFrente(verificationId);
        if(!doc.getId().equals(sessao.getDocumentoEvidenciaId())) {
            concluir(sessao,StatusSessaoBiometria.INCONCLUSIVA);
            return SessaoBiometriaResponse.from(sessao);
        }
        // Arquivo só é lido após controle de acesso; nenhuma imagem/score vai ao cliente.
        var outcome=provider.consultar(sessionId,storage.read(doc.getArquivoChave()),
                livenessThreshold,faceThreshold);
        if(outcome==BiometricProvider.Outcome.APROVADA_TECNICAMENTE) {
            concluir(sessao,StatusSessaoBiometria.APROVADA_TECNICAMENTE);
        } else if(outcome==BiometricProvider.Outcome.INCONCLUSIVA) {
            concluir(sessao,StatusSessaoBiometria.INCONCLUSIVA);
        }
        return SessaoBiometriaResponse.from(sessao);
    }

    private void concluir(SessaoBiometria sessao, StatusSessaoBiometria status) {
        sessao.setStatus(status);
        sessao.setFinalizadoEm(now());
        sessoes.save(sessao);
    }

    /**
     * Política: o match técnico não aprova a verificação, apenas permite enviar
     * a solicitação para revisão documental por um humano. Foto precisa ser a
     * mesma evidência que existia durante a comparação.
     */
    @Transactional(readOnly = true)
    public void exigirBiometriaParaEnvio(Verificacao verificacao) {
        if(!enabled || verificacao.getTipo()!=TipoVerificacao.IDENTIDADE) return;
        EvidenciaVerificacao doc=fotoFrente(verificacao.getId());
        boolean aprovado=sessoes.existsByVerificacaoIdAndDocumentoEvidenciaIdAndStatusAndFinalizadoEmAfter(
            verificacao.getId(),doc.getId(),StatusSessaoBiometria.APROVADA_TECNICAMENTE,
            now().minusHours(24));
        if(!aprovado) {
            throw new VerificacaoIndisponivelException("Prova de vida e comparação facial precisam ser concluídas");
        }
    }

    /**
     * Aprovação administrativa dias após o envio continua válida apenas se
     * houve biometria recente quando o titular enviou o formulário.
     */
    @Transactional(readOnly = true)
    public void exigirBiometriaNaRevisao(Verificacao verificacao) {
        if(!enabled || verificacao.getTipo()!=TipoVerificacao.IDENTIDADE) return;
        EvidenciaVerificacao doc=fotoFrente(verificacao.getId());
        if(verificacao.getEnviadoEm()==null
                || !sessoes.existsByVerificacaoIdAndDocumentoEvidenciaIdAndStatusAndFinalizadoEmAfter(
                    verificacao.getId(), doc.getId(),StatusSessaoBiometria.APROVADA_TECNICAMENTE,
                    verificacao.getEnviadoEm().minusHours(24))) {
            throw new VerificacaoIndisponivelException("Biometria não validada na submissão");
        }
    }
}
