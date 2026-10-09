package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.repository.AnuncioFotoRepository;
import com.josenetoo_dev.veiculos_api.dto.anuncio_dto.AnuncioRequest;
import com.josenetoo_dev.veiculos_api.dto.anuncio_dto.AnuncioResponse;
import com.josenetoo_dev.veiculos_api.enums.Categoria;
import com.josenetoo_dev.veiculos_api.enums.StatusAnuncio;
import com.josenetoo_dev.veiculos_api.exception.ex.AcessoNegadoException;
import com.josenetoo_dev.veiculos_api.exception.ex.AnuncioNaoEncontradoException;
import com.josenetoo_dev.veiculos_api.exception.ex.CredenciaisInvalidasException;
import com.josenetoo_dev.veiculos_api.model.Anuncio;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.AnuncioRepository;
import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnuncioService {

    private final AnuncioRepository     anuncioRepository;
    private final UsuarioRepository usuarioRepository;
    private final AnuncioFotoRepository anuncioFotoRepository;
    private final ImageStorage imageStorage;
    private final VerificacaoService verificacaoService;
    private final com.josenetoo_dev.veiculos_api.repository.PropostaRepository propostaRepository;
    private final com.josenetoo_dev.veiculos_api.repository.MensagemRepository mensagemRepository;

    // Monta o response já com a URL da foto capa (ordem 0)
    private AnuncioResponse toResponse(Anuncio anuncio) {
        String fotoCapa = anuncioFotoRepository
                .findFirstByAnuncioIdOrderByOrdemAsc(anuncio.getId())
                .map(f -> f.getUrl())
                .orElse(null);
        return new AnuncioResponse(anuncio, fotoCapa);
    }

    private Anuncio verificarId(Long id) {
        return anuncioRepository.findById(id)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Anuncio não encontrado"));
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

    private void exigirDonoDoAnuncio(Anuncio anuncio, Usuario usuario) {
        if (!anuncio.getUsuario().getId().equals(usuario.getId())) {
            throw new AcessoNegadoException("Somente o dono do anúncio pode alterá-lo");
        }
    }

    /** Dados não publicados só podem ser consultados pelo vendedor ou administração. */
    private void exigirVisibilidade(Anuncio anuncio) {
        if (anuncio.getStatus() == StatusAnuncio.ATIVO) return;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AnuncioNaoEncontradoException("Anuncio não encontrado");
        }
        boolean staff = authentication.getAuthorities().stream().anyMatch(authority ->
                "ROLE_ADMIN".equals(authority.getAuthority()) || "ROLE_REVIEWER".equals(authority.getAuthority()));
        if (!staff && !anuncio.getUsuario().getId().toString().equals(authentication.getName())) {
            throw new AnuncioNaoEncontradoException("Anuncio não encontrado");
        }
    }

    /** Também utilizado pelo endpoint público de fotos para impedir enumeração de anúncios pausados. */
    @Transactional(readOnly = true)
    public void exigirVisibilidadePorId(Long anuncioId) {
        exigirVisibilidade(verificarId(anuncioId));
    }

    @Transactional(readOnly = true)
    public Page<AnuncioResponse> meusAnuncios(Pageable pageable) {
        return anuncioRepository.findByUsuarioId(obterUsuarioAutenticado().getId(), pageable).map(this::toResponse);
    }

    @Transactional
    public AnuncioResponse criarAnuncio(AnuncioRequest request) {
        Anuncio anuncio = new Anuncio();

        Usuario usuario = obterUsuarioAutenticado();

        anuncio.setUsuario(usuario);

        anuncio.setVersao(request.getVersao());
        anuncio.setLaudoCautelar(request.getLaudoCautelar());
        anuncio.setDocumentacao(request.getDocumentacao());
        anuncio.setGarantia(request.getGarantia());
        anuncio.setTitulo(request.getTitulo());
        anuncio.setDescricao(request.getDescricao());
        anuncio.setPreco(request.getPreco());
        anuncio.setMarca(request.getMarca());
        anuncio.setModelo(request.getModelo());
        anuncio.setAno(request.getAno());
        anuncio.setQuilometragem(request.getQuilometragem());
        anuncio.setCor(request.getCor());
        anuncio.setCombustivel(request.getCombustivel());
        anuncio.setSegundaMao(request.isSegundaMao());
        // Nenhum anúncio novo é publicado automaticamente.
        anuncio.setStatus(StatusAnuncio.PENDENTE);
        anuncio.setCambio(request.getCambio());
        anuncio.setCategoria(request.getCategoria());
        // Veiculo e campos legados do anuncio são persistidos na mesma transação.
        anuncio.sincronizarVeiculo();

        Anuncio anuncioSalvo = anuncioRepository.save(anuncio);

        anuncioSalvo.setCodigo("AM-" + anuncioSalvo.getId());

        return toResponse(anuncioRepository.save(anuncioSalvo));
    }

    /**
     * Modificar fotos de anúncio aprovado invalida sua moderação.
     * Executado na mesma transação da alteração de mídia.
     */
    @Transactional
    public void reabrirRevisaoDeFotos(Anuncio anuncio) {
        if (anuncio.getStatus() != StatusAnuncio.PENDENTE) {
            anuncio.setStatus(StatusAnuncio.PENDENTE);
            anuncio.setRevisadoEm(null);
            anuncio.setRevisadoPorId(null);
            anuncio.setMotivoRejeicao(null);
            anuncioRepository.save(anuncio);
        }
    }

    @Transactional(readOnly = true)
    public Page<AnuncioResponse> listarAnuncios(Pageable pageable) {
        return anuncioRepository.findByStatus(StatusAnuncio.ATIVO, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public AnuncioResponse atualizarAnuncio(AnuncioRequest request, Long id) {
        Anuncio anuncio = anuncioRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Anuncio não encontrado"));
        exigirDonoDoAnuncio(anuncio, obterUsuarioAutenticado());
        if (anuncio.getStatus() == StatusAnuncio.VENDIDO) {
            throw new com.josenetoo_dev.veiculos_api.exception.ex.AnuncioIndisponivelException(
                    "Anúncio vendido não pode ser editado");
        }

        boolean mudouVeiculo = !java.util.Objects.equals(anuncio.getVersao(), request.getVersao())
                || !java.util.Objects.equals(anuncio.getMarca(), request.getMarca())
                || !java.util.Objects.equals(anuncio.getModelo(), request.getModelo())
                || !java.util.Objects.equals(anuncio.getAno(), request.getAno())
                || anuncio.getQuilometragem() != request.getQuilometragem()
                || !java.util.Objects.equals(anuncio.getCor(), request.getCor())
                || anuncio.getCombustivel() != request.getCombustivel()
                || anuncio.getCambio() != request.getCambio()
                || anuncio.isSegundaMao() != request.isSegundaMao();
        if (mudouVeiculo && anuncio.getVeiculo() != null) {
            verificacaoService.invalidarVeiculoAlterado(anuncio.getVeiculo().getId());
        }

        anuncio.setVersao(request.getVersao());
        anuncio.setLaudoCautelar(request.getLaudoCautelar());
        anuncio.setDocumentacao(request.getDocumentacao());
        anuncio.setGarantia(request.getGarantia());
        anuncio.setTitulo(request.getTitulo());
        anuncio.setDescricao(request.getDescricao());
        anuncio.setPreco(request.getPreco());
        anuncio.setMarca(request.getMarca());
        anuncio.setModelo(request.getModelo());
        anuncio.setAno(request.getAno());
        anuncio.setQuilometragem(request.getQuilometragem());
        anuncio.setCor(request.getCor());
        anuncio.setCombustivel(request.getCombustivel());
        anuncio.setSegundaMao(request.isSegundaMao());
        // Qualquer edição exige nova moderação; o próprio vendedor não aprova.
        anuncio.setStatus(StatusAnuncio.PENDENTE);
        anuncio.setRevisadoEm(null);
        anuncio.setRevisadoPorId(null);
        anuncio.setMotivoRejeicao(null);
        anuncio.setCambio(request.getCambio());
        anuncio.setCategoria(request.getCategoria());
        anuncio.sincronizarVeiculo();

        return toResponse(anuncioRepository.save(anuncio));
    }

    @Transactional
    public void deletarAnuncio(Long id) {
        Anuncio anuncio = verificarId(id);
        exigirDonoDoAnuncio(anuncio, obterUsuarioAutenticado());
        // Remove mensagens e propostas vinculadas (chave estrangeira), nessa ordem
        mensagemRepository.deleteByPropostaAnuncioId(id);
        propostaRepository.deleteByAnuncioId(id);
        anuncioFotoRepository.findByAnuncioId(id,
                        org.springframework.data.domain.Pageable.unpaged())
                .forEach(f -> {
                    anuncioFotoRepository.delete(f);
                    imageStorage.deleteAfterCommit(f.getUrl());
                });
        anuncioRepository.delete(anuncio);
    }

    @Transactional(readOnly = true)
    public AnuncioResponse buscarPorId(Long id) {
        Anuncio anuncio = verificarId(id);
        exigirVisibilidade(anuncio);
        return toResponse(anuncio);
    }

    @Transactional(readOnly = true)
    public AnuncioResponse buscarPorCodigo(String codigo) {
        Anuncio anuncio = anuncioRepository.findByCodigo(codigo)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Anuncio não encontrado"));
        exigirVisibilidade(anuncio);
        return toResponse(anuncio);
    }

    @Transactional(readOnly = true)
    public Page<AnuncioResponse> listarDestaques(Pageable pageable) {
        return anuncioRepository.findByDestaqueTrueAndStatus(StatusAnuncio.ATIVO, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AnuncioResponse> listarPorStatus(StatusAnuncio status, Pageable pageable) {
        if (status == StatusAnuncio.ATIVO) {
            return anuncioRepository.findByStatus(status, pageable).map(this::toResponse);
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Page.empty(pageable);
        }
        boolean staff = auth.getAuthorities().stream().anyMatch(authority ->
                "ROLE_ADMIN".equals(authority.getAuthority()) || "ROLE_REVIEWER".equals(authority.getAuthority()));
        if (staff) return anuncioRepository.findByStatus(status, pageable).map(this::toResponse);
        try {
            Long userId = Long.valueOf(auth.getName());
            return anuncioRepository.findByStatusAndUsuarioId(status, userId, pageable).map(this::toResponse);
        } catch (NumberFormatException e) {
            return Page.empty(pageable);
        }
    }

    @Transactional(readOnly = true)
    public Page<AnuncioResponse> listarPorCategoria(Categoria categoria, Pageable pageable) {
        return anuncioRepository.findByCategoriaAndStatus(categoria, StatusAnuncio.ATIVO, pageable)
                .map(this::toResponse);
    }

}