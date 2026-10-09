package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.verificacao.*;
import com.josenetoo_dev.veiculos_api.enums.*;
import com.josenetoo_dev.veiculos_api.exception.ex.*;
import com.josenetoo_dev.veiculos_api.model.*;
import com.josenetoo_dev.veiculos_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VerificacaoService {
    private final VerificacaoRepository verificacoes;
    private final EvidenciaVerificacaoRepository evidencias;
    private final EventoVerificacaoRepository eventos;
    private final UsuarioRepository usuarios;
    private final VeiculoRepository veiculos;
    private final PrivateEvidenceStorage storage;
    private final ImageValidator validator;

    private Usuario actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new CredenciaisInvalidasException("Não autenticado");
        }
        try {
            Usuario usuario = usuarios.findById(Long.valueOf(authentication.getName()))
                    .orElseThrow(() -> new CredenciaisInvalidasException("Conta não encontrada"));
            if (!usuario.getStatus().podeAutenticar()) {
                throw new CredenciaisInvalidasException("Conta indisponível");
            }
            return usuario;
        } catch (NumberFormatException e) {
            throw new CredenciaisInvalidasException("Credenciais inválidas");
        }
    }

    private static boolean staff(Usuario user) {
        return user.getStatus() == StatusUsuario.ACTIVE
                && (user.getRole() == Role.ADMIN || user.getRole() == Role.REVIEWER);
    }

    private Usuario reviewer() {
        Usuario user = actor();
        if (!staff(user)) throw new AcessoNegadoException("Revisor ativo obrigatório");
        return user;
    }

    private void requireVisible(Verificacao v, Usuario viewer) {
        if (!v.getSolicitante().getId().equals(viewer.getId()) && !staff(viewer)) {
            throw new AcessoNegadoException("Acesso à verificação negado");
        }
    }

    private Verificacao ownedCaseLocked(Long id, Usuario actor) {
        Verificacao v = verificacoes.findByIdForUpdate(id)
                .orElseThrow(() -> new VerificacaoNaoEncontradaException());
        if (!v.getSolicitante().getId().equals(actor.getId())) {
            throw new AcessoNegadoException("Verificação de outro usuário");
        }
        return v;
    }

    private static void editable(Verificacao v) {
        if (v.getStatus() != StatusVerificacao.RASCUNHO && v.getStatus() != StatusVerificacao.REJEITADA) {
            throw new VerificacaoIndisponivelException("Verificação não pode ser alterada neste estado");
        }
    }

    private void requireEvidence(Verificacao v) {
        var kinds = evidencias.findByVerificacaoId(v.getId()).stream()
                .map(EvidenciaVerificacao::getTipo).collect(java.util.stream.Collectors.toSet());
        if (v.getTipo() == TipoVerificacao.IDENTIDADE) {
            if (!kinds.contains(TipoEvidencia.IDENTIDADE_FRENTE)
                    || !kinds.contains(TipoEvidencia.IDENTIDADE_VERSO)) {
                throw new IllegalArgumentException("Envie frente e verso do documento de identidade");
            }
        } else if (!kinds.contains(TipoEvidencia.DOCUMENTO_VEICULO)) {
            throw new IllegalArgumentException("Envie imagem do documento do veículo");
        }
    }

    @Transactional
    public VerificacaoResponse iniciarIdentidade() {
        Usuario usuario = actor();
        return VerificacaoResponse.from(verificacoes.findByUsuarioIdentidadeId(usuario.getId())
                .orElseGet(() -> verificacoes.saveAndFlush(Verificacao.identidade(usuario))));
    }

    @Transactional
    public VerificacaoResponse iniciarVeiculo(Long veiculoId) {
        Usuario user = actor();
        Veiculo vehicle = veiculos.findById(veiculoId)
                .orElseThrow(() -> new VerificacaoNaoEncontradaException());
        if (vehicle.getCadastradoPor() == null
                || !vehicle.getCadastradoPor().getId().equals(user.getId())) {
            throw new AcessoNegadoException("Somente o responsável pelo cadastro pode solicitar a análise");
        }
        return VerificacaoResponse.from(verificacoes.findByVeiculoId(veiculoId)
                .orElseGet(() -> verificacoes.saveAndFlush(Verificacao.veiculo(user, vehicle))));
    }

    @Transactional(readOnly = true)
    public Page<VerificacaoResponse> minhas(Pageable pageable) {
        return verificacoes.findBySolicitanteId(actor().getId(), pageable).map(VerificacaoResponse::from);
    }

    @Transactional(readOnly = true)
    public VerificacaoResponse consultar(Long id) {
        Usuario user = actor();
        Verificacao v = verificacoes.findById(id)
                .orElseThrow(() -> new VerificacaoNaoEncontradaException());
        requireVisible(v, user);
        return VerificacaoResponse.from(v);
    }

    @Transactional(readOnly = true)
    public List<EvidenciaResponse> listarEvidencias(Long id) {
        Usuario user = actor();
        Verificacao v = verificacoes.findById(id)
                .orElseThrow(() -> new VerificacaoNaoEncontradaException());
        requireVisible(v, user);
        return evidencias.findByVerificacaoId(id).stream().map(EvidenciaResponse::from).toList();
    }

    @Transactional
    public EvidenciaResponse adicionarEvidencia(Long id, TipoEvidencia kind, MultipartFile image) {
        Usuario user = actor();
        Verificacao v = ownedCaseLocked(id, user);
        editable(v);
        if ((v.getTipo() == TipoVerificacao.IDENTIDADE && kind == TipoEvidencia.DOCUMENTO_VEICULO)
                || (v.getTipo() == TipoVerificacao.VEICULO && kind != TipoEvidencia.DOCUMENTO_VEICULO)) {
            throw new IllegalArgumentException("Tipo de documento incompatível com a solicitação");
        }
        if (evidencias.existsByVerificacaoIdAndTipo(id, kind)) {
            throw new VerificacaoIndisponivelException("Documento já enviado; exclua-o antes de substituir");
        }
        if (!storage.available()) throw new ArmazenamentoPrivadoIndisponivelException();
        ImageValidator.ValidatedImage validated = validator.validate(image);
        EvidenciaVerificacao evidence = new EvidenciaVerificacao();
        evidence.setVerificacao(v);
        evidence.setTipo(kind);
        evidence.setArquivoChave(storage.store(validated));
        evidence.setTipoMidia("jpg".equals(validated.extension()) ? "image/jpeg" : "image/png");
        evidence.setTamanho((long) validated.bytes().length);
        return EvidenciaResponse.from(evidencias.saveAndFlush(evidence));
    }

    @Transactional
    public void excluirEvidencia(Long id, Long evidenceId) {
        Usuario user = actor();
        Verificacao v = ownedCaseLocked(id, user);
        editable(v);
        EvidenciaVerificacao evidence = evidencias.findById(evidenceId)
                .orElseThrow(() -> new VerificacaoNaoEncontradaException());
        if (!evidence.getVerificacao().getId().equals(id)) {
            throw new VerificacaoNaoEncontradaException();
        }
        evidencias.delete(evidence);
        storage.removeAfterCommit(evidence.getArquivoChave());
    }

    public record DocumentData(byte[] bytes, String mime) {}

    @Transactional(readOnly = true)
    public DocumentData baixarEvidencia(Long id, Long evidenceId) {
        Usuario user = actor();
        Verificacao v = verificacoes.findById(id)
                .orElseThrow(() -> new VerificacaoNaoEncontradaException());
        requireVisible(v, user);
        EvidenciaVerificacao evidence = evidencias.findById(evidenceId)
                .orElseThrow(() -> new VerificacaoNaoEncontradaException());
        if (!evidence.getVerificacao().getId().equals(id)) {
            throw new VerificacaoNaoEncontradaException();
        }
        return new DocumentData(storage.read(evidence.getArquivoChave()), evidence.getTipoMidia());
    }

    @Transactional
    public VerificacaoResponse enviar(Long id) {
        Usuario user = actor();
        Verificacao v = ownedCaseLocked(id, user);
        editable(v);
        requireEvidence(v);
        if (v.getTipo() == TipoVerificacao.VEICULO
                && !verificacoes.existsByUsuarioIdentidadeIdAndStatus(
                    user.getId(), StatusVerificacao.APROVADA)) {
            throw new VerificacaoIndisponivelException("Identidade deve ser aprovada antes da análise do veículo");
        }
        v.setStatus(StatusVerificacao.EM_ANALISE);
        v.setEnviadoEm(LocalDateTime.now(ZoneOffset.UTC));
        v.setMotivoRejeicao(null);
        v.setRevisadoEm(null);
        v.setRevisadoPorId(null);
        return VerificacaoResponse.from(verificacoes.save(v));
    }

    @Transactional(readOnly = true)
    public Page<VerificacaoResponse> pendentes(Pageable pageable) {
        reviewer();
        return verificacoes.findByStatus(StatusVerificacao.EM_ANALISE, pageable)
                .map(VerificacaoResponse::from);
    }

    @Transactional
    public VerificacaoResponse decidir(Long id, boolean aprovado, String motivo) {
        Usuario staff = reviewer();
        Verificacao v = verificacoes.findByIdForUpdate(id)
                .orElseThrow(() -> new VerificacaoNaoEncontradaException());
        if (v.getStatus() != StatusVerificacao.EM_ANALISE) {
            throw new VerificacaoIndisponivelException("Verificação não está aguardando análise");
        }
        if (v.getSolicitante().getId().equals(staff.getId())) {
            throw new AcessoNegadoException("Não é permitido analisar o próprio documento");
        }
        if (!aprovado && (motivo == null || motivo.isBlank() || motivo.strip().length() > 500)) {
            throw new IllegalArgumentException("Motivo de rejeição obrigatório");
        }
        requireEvidence(v);
        if (aprovado && v.getTipo() == TipoVerificacao.VEICULO
                && !verificacoes.existsByUsuarioIdentidadeIdAndStatus(
                    v.getSolicitante().getId(), StatusVerificacao.APROVADA)) {
            throw new VerificacaoIndisponivelException("Identidade do solicitante ainda não aprovada");
        }
        StatusVerificacao result = aprovado ? StatusVerificacao.APROVADA : StatusVerificacao.REJEITADA;
        String reason = aprovado ? null : motivo.strip();
        v.setStatus(result);
        v.setMotivoRejeicao(reason);
        v.setRevisadoEm(LocalDateTime.now(ZoneOffset.UTC));
        v.setRevisadoPorId(staff.getId());
        eventos.save(new EventoVerificacao(v.getId(), staff.getId(), result, reason));
        return VerificacaoResponse.from(verificacoes.save(v));
    }
}
