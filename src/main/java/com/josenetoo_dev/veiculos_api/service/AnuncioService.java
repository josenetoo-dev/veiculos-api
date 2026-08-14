package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.anuncio_dto.AnuncioRequest;
import com.josenetoo_dev.veiculos_api.dto.anuncio_dto.AnuncioResponse;
import com.josenetoo_dev.veiculos_api.enums.Categoria;
import com.josenetoo_dev.veiculos_api.enums.StatusAnuncio;
import com.josenetoo_dev.veiculos_api.exception.AnuncioNaoEncontradoException;
import com.josenetoo_dev.veiculos_api.exception.UsuarioNaoEncontradoException;
import com.josenetoo_dev.veiculos_api.model.Anuncio;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.AnuncioRepository;
import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnuncioService {

    private final AnuncioRepository anuncioRepository;
    private final UsuarioRepository usuarioRepository;

    // CRUD basico + buscar por codigo, destaque, status e categoria

    // Verificar por id
    private Anuncio verificarId(Long id) {
        return anuncioRepository.findById(id)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Anuncio não encontrado"));
    }

    // Create
    @Transactional
    public AnuncioResponse criarAnuncio(AnuncioRequest request) {
        Anuncio anuncio = new Anuncio();

        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                        .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuario não encontrado"));

        // TODO: Esse função deve ser trocada após a ingrassão da autenticação
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

        return new AnuncioResponse(anuncioRepository.save(anuncioSalvo));
    }

    // Read com paginação
    @Transactional(readOnly = true)
    public Page<AnuncioResponse> listarAnuncios(Pageable pageable) {
        return anuncioRepository.findAll(pageable)
                .map(AnuncioResponse::new);
    }

    // Update
    @Transactional
    public AnuncioResponse atualizarAnuncio(AnuncioRequest request, Long id) {
        Anuncio anuncio = verificarId(id);

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

        return new AnuncioResponse(anuncioRepository.save(anuncio));
    }

    // Delete
    @Transactional
    public void deletarAnuncio(Long id) {
        Anuncio anuncio = verificarId(id);
        anuncioRepository.delete(anuncio);
    }

    // buscar por id
    @Transactional(readOnly = true)
    public AnuncioResponse buscarPorId(Long id) {
        return new AnuncioResponse(verificarId(id));
    }

    // buscar por codigo
    @Transactional(readOnly = true)
    public AnuncioResponse buscarPorCodigo(String codigo) {
        return anuncioRepository.findByCodigo(codigo)
                .map(AnuncioResponse::new)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Anuncio não encontrado"));
    }

    // listar destaques
    @Transactional(readOnly = true)
    public Page<AnuncioResponse> listarDestaques(Pageable pageable) {
        return anuncioRepository.findByDestaqueTrue(pageable)
                .map(AnuncioResponse::new);
    }

    // listar por status
    @Transactional(readOnly = true)
    public Page<AnuncioResponse> listarPorStatus(StatusAnuncio status, Pageable pageable) {
        return anuncioRepository.findByStatus(status, pageable)
                .map(AnuncioResponse::new);
    }

    // listar por categoria
    @Transactional(readOnly = true)
    public Page<AnuncioResponse> listarPorCategoria(Categoria categoria, Pageable pageable) {
        return anuncioRepository.findByCategoria(categoria, pageable)
                .map(AnuncioResponse::new);
    }

}
