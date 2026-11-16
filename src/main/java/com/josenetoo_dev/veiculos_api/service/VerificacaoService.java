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
    private final BiometriaService biometriaService;
    private final AnuncioRepository anuncios;
    private final ModeracaoEventoRepository moderacaoEventos;

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
        if (v.getSolicitante().getId().equals(viewer.getId())) return;
        // Operadores não acessam rascunhos que o usuário ainda não enviou.
        if (!staff(viewer) || v.getStatus() == StatusVerificacao.RASCUNHO) {
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
        if (!v.getStatus().editavel()) {
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
        if (!storage.available()) throw new ArmazenamentoPrivadoIndisponivelException();
        ImageValidator.ValidatedImage validated = validator.validate(image);
        // Cada arquivo tem uma identidade própria: biometria/dispensas anteriores
        // nunca podem ser reaproveitadas depois da substituição do documento.
        var previous = evidencias.findByVerificacaoId(id).stream()
                .filter(existing -> existing.getTipo() == kind).findFirst();
        String newKey = storage.store(validated);
        previous.ifPresent(existing -> { evidencias.delete(existing); evidencias.flush(); });
        EvidenciaVerificacao evidence = new EvidenciaVerificacao();
        evidence.setVerificacao(v);
        evidence.setTipo(kind);
        evidence.setArquivoChave(newKey);
        evidence.setTipoMidia("jpg".equals(validated.extension()) ? "image/jpeg" : "image/png");
        evidence.setTamanho((long) validated.bytes().length);
        var response = EvidenciaResponse.from(evidencias.saveAndFlush(evidence));
        previous.ifPresent(existing -> storage.removeAfterCommit(existing.getArquivoChave()));
        return response;
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
        if (v.getStatus() == StatusVerificacao.EM_ANALISE) return VerificacaoResponse.from(v);
        editable(v);
        requireEvidence(v);
        biometriaService.exigirBiometriaParaEnvio(v);
        v.setStatus(StatusVerificacao.EM_ANALISE);
        v.setEnviadoEm(LocalDateTime.now(ZoneOffset.UTC));
        v.setMotivoRejeicao(null);
        v.setCategoriaResultado(null);
        v.setRevisadoEm(null);
        v.setRevisadoPorId(null);
        return VerificacaoResponse.from(verificacoes.save(v));
    }

    /** Só encaminha para a fila humana: não dispensa biometria nem aprova documentos. */
    @Transactional
    public VerificacaoResponse solicitarAnaliseManual(Long id) {
        Usuario user = actor();
        Verificacao v = ownedCaseLocked(id, user);
        if (v.getStatus().aguardandoAnalise()) return VerificacaoResponse.from(v);
        editable(v);
        requireEvidence(v);
        v.setStatus(StatusVerificacao.INCONCLUSIVA);
        v.setCategoriaResultado(CategoriaResultadoVerificacao.VERIFICACAO_INCONCLUSIVA);
        v.setMotivoRejeicao("Análise manual solicitada. O anúncio permanece pendente até a conclusão das verificações.");
        v.setEnviadoEm(LocalDateTime.now(ZoneOffset.UTC));
        v.setRevisadoEm(null);
        v.setRevisadoPorId(null);
        eventos.save(new EventoVerificacao(id, user.getId(), StatusVerificacao.INCONCLUSIVA,
                "Solicitação de análise manual pelo titular; sem aprovação ou dispensa automática",
                CategoriaResultadoVerificacao.VERIFICACAO_INCONCLUSIVA));
        return VerificacaoResponse.from(verificacoes.save(v));
    }

    /**
     * Invalida documento antigo caso as características do veículo sejam alteradas.
     * Exclui evidências após commit para exigir nova apresentação documental.
     * Chamado somente dentro da transação que atualiza o anúncio.
     */
    @Transactional
    public void invalidarVeiculoAlterado(Long veiculoId) {
        if (veiculoId == null) return;
        verificacoes.findByVeiculoId(veiculoId).ifPresent(v -> {
            Verificacao locked = verificacoes.findByIdForUpdate(v.getId()).orElseThrow();
            for (EvidenciaVerificacao e : evidencias.findByVerificacaoId(locked.getId())) {
                evidencias.delete(e);
                storage.removeAfterCommit(e.getArquivoChave());
            }
            locked.setStatus(StatusVerificacao.RASCUNHO);
            locked.setMotivoRejeicao(null);
            locked.setCategoriaResultado(null);
            locked.setEnviadoEm(null);
            locked.setRevisadoEm(null);
            locked.setRevisadoPorId(null);
            verificacoes.save(locked);
        });
    }

    @Transactional(readOnly = true)
    public Page<VerificacaoResponse> pendentes(Pageable pageable) {
        return pendentes(pageable, null);
    }

    @Transactional(readOnly = true)
    public Page<VerificacaoResponse> pendentes(Pageable pageable, StatusVerificacao status) {
        reviewer();
        if (status == StatusVerificacao.RASCUNHO) {
            throw new IllegalArgumentException("Filtro de revisão inválido");
        }
        return (status == null
                ? verificacoes.findByStatusIn(List.of(StatusVerificacao.EM_ANALISE, StatusVerificacao.INCONCLUSIVA), pageable)
                : verificacoes.findByStatus(status, pageable))
                .map(VerificacaoResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<EventoVerificacaoResponse> historico(Long id, Pageable pageable) {
        reviewer();
        Verificacao v = verificacoes.findById(id).orElseThrow(VerificacaoNaoEncontradaException::new);
        requireVisible(v, actor());
        return eventos.findByVerificacaoIdOrderByCriadoEmDescIdDesc(id, pageable).map(event ->
                new EventoVerificacaoResponse(usuarios.findById(event.getRevisorId())
                        .map(Usuario::getNome).orElse("Operador indisponível"), event.getResultado(),
                        event.getCategoriaResultado(), event.getMotivo(), event.getCriadoEm()));
    }

    /** Correção e inconclusão preservam evidências; nunca equivalem a fraude ou aprovação. */
    @Transactional
    public VerificacaoResponse reavaliar(Long id, StatusVerificacao resultado, ReavaliarVerificacaoRequest request) {
        Usuario operador = reviewer();
        if (operador.getRole() != Role.ADMIN) throw new AcessoNegadoException("Somente a administração pode solicitar reavaliação");
        if (!List.of(StatusVerificacao.CORRECAO_NECESSARIA, StatusVerificacao.INCONCLUSIVA,
                StatusVerificacao.EXPIRADA).contains(resultado)) throw new IllegalArgumentException("Decisão inválida");
        if (request == null || request.motivo() == null || request.motivo().strip().length() < 10
                || request.motivo().strip().length() > 500 || request.categoria() == null
                || request.categoria() == CategoriaResultadoVerificacao.FRAUDE_CONFIRMADA) {
            throw new IllegalArgumentException("Informe uma justificativa e uma classificação compatível com a reavaliação");
        }
        Verificacao v = verificacoes.findByIdForUpdate(id).orElseThrow(VerificacaoNaoEncontradaException::new);
        if (v.getSolicitante().getId().equals(operador.getId())) throw new AcessoNegadoException("Não é permitido analisar o próprio documento");
        String motivo = request.motivo().strip();
        if (v.getStatus() == resultado && motivo.equals(v.getMotivoRejeicao())
                && v.getCategoriaResultado() == request.categoria()) return VerificacaoResponse.from(v);
        if (resultado == StatusVerificacao.EXPIRADA ? v.getStatus() != StatusVerificacao.APROVADA
                : !v.getStatus().aguardandoAnalise()) throw new VerificacaoIndisponivelException("O estado atual não permite esta decisão");
        v.setStatus(resultado);
        v.setMotivoRejeicao(motivo);
        v.setCategoriaResultado(request.categoria());
        v.setRevisadoEm(LocalDateTime.now(ZoneOffset.UTC));
        v.setRevisadoPorId(operador.getId());
        eventos.save(new EventoVerificacao(id, operador.getId(), resultado, motivo, request.categoria()));
        if (resultado == StatusVerificacao.EXPIRADA) {
            var afetados = v.getTipo() == TipoVerificacao.IDENTIDADE
                    ? anuncios.findByUsuarioIdAndStatus(v.getSolicitante().getId(), StatusAnuncio.ATIVO)
                    : anuncios.findByVeiculoIdAndStatus(v.getVeiculo().getId(), StatusAnuncio.ATIVO);
            for (var candidato : afetados) {
                var anuncio = anuncios.findByIdForUpdate(candidato.getId()).orElseThrow();
                if (anuncio.getStatus() != StatusAnuncio.ATIVO) continue;
                anuncio.setStatus(StatusAnuncio.PENDENTE);
                anuncio.setRevisadoEm(null);
                anuncio.setRevisadoPorId(null);
                anuncio.setMotivoRejeicao(null);
                moderacaoEventos.save(new ModeracaoEvento(anuncio.getId(), operador.getId(), StatusAnuncio.ATIVO,
                        StatusAnuncio.PENDENTE, "Verificação documental exige revalidação"));
            }
        }
        return VerificacaoResponse.from(verificacoes.save(v));
    }

    @Transactional
    public VerificacaoResponse decidir(Long id, boolean aprovado, String motivo) {
        return decidir(id, aprovado, motivo, null);
    }

    @Transactional
    public VerificacaoResponse decidir(Long id, boolean aprovado, String motivo, CategoriaResultadoVerificacao categoria) {
        Usuario staff = reviewer();
        if (!aprovado && categoria == CategoriaResultadoVerificacao.FALHA_TECNICA) {
            throw new IllegalArgumentException("Falhas técnicas devem permanecer em análise manual, sem rejeição por fraude");
        }
        if (categoria == CategoriaResultadoVerificacao.FRAUDE_CONFIRMADA && staff.getRole() != Role.ADMIN) {
            throw new AcessoNegadoException("Somente a administração pode registrar fraude confirmada");
        }
        Verificacao v = verificacoes.findByIdForUpdate(id)
                .orElseThrow(() -> new VerificacaoNaoEncontradaException());
        if (!v.getStatus().aguardandoAnalise()) {
            throw new VerificacaoIndisponivelException("Verificação não está aguardando análise");
        }
        if (v.getSolicitante().getId().equals(staff.getId())) {
            throw new AcessoNegadoException("Não é permitido analisar o próprio documento");
        }
        if (!aprovado && (motivo == null || motivo.isBlank() || motivo.strip().length() > 500)) {
            throw new IllegalArgumentException("Motivo de rejeição obrigatório");
        }
        requireEvidence(v);
        if (aprovado) biometriaService.exigirBiometriaNaRevisao(v);
        if (aprovado && v.getTipo() == TipoVerificacao.VEICULO
                && !verificacoes.existsByUsuarioIdentidadeIdAndStatus(
                    v.getSolicitante().getId(), StatusVerificacao.APROVADA)) {
            throw new VerificacaoIndisponivelException("Identidade do solicitante ainda não aprovada");
        }
        StatusVerificacao result = aprovado ? StatusVerificacao.APROVADA : StatusVerificacao.REJEITADA;
        String reason = aprovado ? null : motivo.strip();
        v.setStatus(result);
        v.setMotivoRejeicao(reason);
        v.setCategoriaResultado(aprovado ? null : categoria);
        v.setRevisadoEm(LocalDateTime.now(ZoneOffset.UTC));
        v.setRevisadoPorId(staff.getId());
        eventos.save(new EventoVerificacao(v.getId(), staff.getId(), result, reason, aprovado ? null : categoria));
        return VerificacaoResponse.from(verificacoes.save(v));
    }
}
