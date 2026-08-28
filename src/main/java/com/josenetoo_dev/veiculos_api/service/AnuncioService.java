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
        anuncio.setStatus(StatusAnuncio.ATIVO);
        anuncio.setCambio(request.getCambio());
        anuncio.setCategoria(request.getCategoria());


        Anuncio anuncioSalvo = anuncioRepository.save(anuncio);

        anuncioSalvo.setCodigo("AM-" + anuncioSalvo.getId());

        return toResponse(anuncioRepository.save(anuncioSalvo));
    }

    @Transactional(readOnly = true)
    public Page<AnuncioResponse> listarAnuncios(Pageable pageable) {
        return anuncioRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Transactional
    public AnuncioResponse atualizarAnuncio(AnuncioRequest request, Long id) {
        Anuncio anuncio = verificarId(id);
        exigirDonoDoAnuncio(anuncio, obterUsuarioAutenticado());

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
        anuncio.setStatus(StatusAnuncio.ATIVO);
        anuncio.setCambio(request.getCambio());
        anuncio.setCategoria(request.getCategoria());

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
                .forEach(f -> anuncioFotoRepository.delete(f));
        anuncioRepository.delete(anuncio);
    }

    @Transactional(readOnly = true)
    public AnuncioResponse buscarPorId(Long id) {
        return toResponse(verificarId(id));
    }

    @Transactional(readOnly = true)
    public AnuncioResponse buscarPorCodigo(String codigo) {
        return anuncioRepository.findByCodigo(codigo)
                .map(this::toResponse)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Anuncio não encontrado"));
    }

    @Transactional(readOnly = true)
    public Page<AnuncioResponse> listarDestaques(Pageable pageable) {
        return anuncioRepository.findByDestaqueTrue(pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AnuncioResponse> listarPorStatus(StatusAnuncio status, Pageable pageable) {
        return anuncioRepository.findByStatus(status, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AnuncioResponse> listarPorCategoria(Categoria categoria, Pageable pageable) {
        return anuncioRepository.findByCategoria(categoria, pageable)
                .map(this::toResponse);
    }

}