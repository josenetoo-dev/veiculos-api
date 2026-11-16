package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.mensagem_dto.MensagemRequest;
import com.josenetoo_dev.veiculos_api.dto.mensagem_dto.MensagemResponse;
import com.josenetoo_dev.veiculos_api.exception.ex.AcessoNegadoException;
import com.josenetoo_dev.veiculos_api.exception.ex.CredenciaisInvalidasException;
import com.josenetoo_dev.veiculos_api.exception.ex.PropostaNaoEncontradaException;
import com.josenetoo_dev.veiculos_api.exception.ex.MensagemNaoEncontradaException;
import com.josenetoo_dev.veiculos_api.model.Mensagem;
import com.josenetoo_dev.veiculos_api.model.Proposta;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.MensagemRepository;
import com.josenetoo_dev.veiculos_api.repository.PropostaRepository;
import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MensagemService {
    private final MensagemRepository mensagemRepository;
    private final PropostaRepository propostaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MensagemDeletionPolicy deletionPolicy;
    private final ImageValidator imageValidator;
    private final PrivateEvidenceStorage privateStorage;

    // Chat vinculado a uma proposta: só o comprador e o dono do anúncio daquela proposta podem participar

    @Transactional
    public MensagemResponse enviarMensagem(Long propostaId, MensagemRequest request) {
        Proposta proposta = buscarProposta(propostaId);
        Usuario usuario = obterUsuarioAutenticado();

        exigirParticipanteDaNegociacao(proposta, usuario);

        Mensagem mensagem = new Mensagem();
        mensagem.setConteudo(request.getConteudo());
        mensagem.setCriadoEm(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        mensagem.setProposta(proposta);
        mensagem.setRemetente(usuario);

        return new MensagemResponse(mensagemRepository.save(mensagem));
    }

    @Transactional
    public MensagemResponse enviarImagem(Long propostaId, MultipartFile file) {
        Proposta proposta = buscarProposta(propostaId);
        Usuario usuario = obterUsuarioAutenticado();
        exigirParticipanteDaNegociacao(proposta, usuario);
        var image = imageValidator.validate(file);
        String key = privateStorage.store(image);
        Mensagem message = new Mensagem();
        message.setConteudo("Imagem anexada");
        message.setCriadoEm(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        message.setProposta(proposta);
        message.setRemetente(usuario);
        message.setArquivoChave(key);
        message.setArquivoNome(safeFilename(file.getOriginalFilename(), image.extension()));
        message.setTipoMidia(image.extension().equals("png") ? "image/png" : "image/jpeg");
        message.setArquivoTamanho((long) image.bytes().length);
        return new MensagemResponse(mensagemRepository.saveAndFlush(message));
    }

    @Transactional(readOnly = true)
    public DownloadedChatImage baixarImagem(Long propostaId, Long mensagemId) {
        Proposta proposta = buscarProposta(propostaId);
        exigirParticipanteDaNegociacao(proposta, obterUsuarioAutenticado());
        Mensagem message = mensagemRepository.findById(mensagemId)
                .filter(item -> item.getProposta().getId().equals(propostaId) && item.getExcluidaEm() == null)
                .orElseThrow(() -> new MensagemNaoEncontradaException("Anexo não encontrado"));
        if (message.getArquivoChave() == null || message.getTipoMidia() == null
                || !java.util.Set.of("image/jpeg", "image/png").contains(message.getTipoMidia())) {
            throw new MensagemNaoEncontradaException("Anexo não encontrado");
        }
        return new DownloadedChatImage(privateStorage.read(message.getArquivoChave()), message.getTipoMidia());
    }

    public record DownloadedChatImage(byte[] bytes, String mime) {}

    private String safeFilename(String original, String extension) {
        String base = original == null ? "imagem" : original.replaceAll("(?i)\\.[a-z0-9]{1,8}$", "")
                .replaceAll("[^A-Za-z0-9_-]", "_");
        if (base.isBlank()) base = "imagem";
        if (base.length() > 100) base = base.substring(0, 100);
        return base + "." + extension;
    }

    @Transactional(readOnly = true)
    public Page<MensagemResponse> listarMensagens(Long propostaId, Pageable pageable) {
        Proposta proposta = buscarProposta(propostaId);
        Usuario usuario = obterUsuarioAutenticado();

        exigirParticipanteDaNegociacao(proposta, usuario);

        return mensagemRepository.findByPropostaIdAndExcluidaEmIsNullOrderByCriadoEmAsc(propostaId, pageable)
                .map(MensagemResponse::new);
    }

    @Transactional
    public void excluirMensagem(Long propostaId, Long mensagemId) {
        Proposta proposta = buscarProposta(propostaId);
        Usuario usuario = obterUsuarioAutenticado();
        exigirParticipanteDaNegociacao(proposta, usuario);
        Mensagem mensagem = mensagemRepository.findById(mensagemId)
                .filter(item -> item.getProposta().getId().equals(propostaId))
                .orElseThrow(() -> new MensagemNaoEncontradaException("Mensagem não encontrada"));
        if (mensagem.getExcluidaEm() != null) return;
        if (!deletionPolicy.canDelete(mensagem.getRemetente().getId(), usuario.getId(), mensagem.getCriadoEm(),
                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC))) {
            throw new AcessoNegadoException("Mensagem não pode ser excluída por este usuário ou após cinco minutos");
        }
        mensagem.setExcluidaEm(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        mensagem.setExcluidaPorId(usuario.getId());
        if (mensagem.getArquivoChave() != null) {
            privateStorage.removeAfterCommit(mensagem.getArquivoChave());
        }
        mensagemRepository.save(mensagem);
    }

    @Transactional(readOnly = true)
    public java.util.Map<Long, Long> pendencias() {
        return mensagemRepository.pendencias(obterUsuarioAutenticado().getId()).stream()
                .collect(java.util.stream.Collectors.toMap(MensagemRepository.Pendencia::getPropostaId,
                        MensagemRepository.Pendencia::getQuantidade));
    }

    @Transactional
    public void marcarLidas(Long propostaId, java.util.List<Long> ids) {
        var usuario = obterUsuarioAutenticado();
        exigirParticipanteDaNegociacao(buscarProposta(propostaId), usuario);
        mensagemRepository.marcarLidas(propostaId, usuario.getId(), ids,
                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
    }

    private Proposta buscarProposta(Long id) {
        return propostaRepository.findById(id)
                .orElseThrow(() -> new PropostaNaoEncontradaException("Proposta não encontrada"));
    }

    private Usuario obterUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new CredenciaisInvalidasException("Usuário não autenticado");
        }

        Long id;
        try {
            id = Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new CredenciaisInvalidasException("Usuário autenticado não encontrado");
        }

        return usuarioRepository.findById(id)
                .orElseThrow(() -> new CredenciaisInvalidasException("Usuário autenticado não encontrado"));
    }

    private void exigirParticipanteDaNegociacao(Proposta proposta, Usuario usuario) {
        boolean ehComprador = proposta.getComprador().getId().equals(usuario.getId());
        boolean ehDonoDoAnuncio = proposta.getAnuncio().getUsuario().getId().equals(usuario.getId());

        if (!ehComprador && !ehDonoDoAnuncio) {
            throw new AcessoNegadoException("Você não faz parte desta negociação");
        }
    }
}
