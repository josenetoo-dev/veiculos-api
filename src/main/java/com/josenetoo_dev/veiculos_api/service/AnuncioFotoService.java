package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.anuncio_foto_dto.AnuncioFotoRequest;
import com.josenetoo_dev.veiculos_api.dto.anuncio_foto_dto.AnuncioFotoResponse;
import com.josenetoo_dev.veiculos_api.exception.ex.AcessoNegadoException;
import com.josenetoo_dev.veiculos_api.exception.ex.AnuncioNaoEncontradoException;
import com.josenetoo_dev.veiculos_api.exception.ex.CredenciaisInvalidasException;
import com.josenetoo_dev.veiculos_api.model.Anuncio;
import com.josenetoo_dev.veiculos_api.model.AnuncioFoto;
import com.josenetoo_dev.veiculos_api.exception.ex.FotoNaoEncontradaException;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.AnuncioFotoRepository;
import com.josenetoo_dev.veiculos_api.repository.AnuncioRepository;
import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnuncioFotoService {

    private final AnuncioFotoRepository anuncioFotoRepository;
    private final AnuncioRepository anuncioRepository;
    private final UsuarioRepository usuarioRepository;

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
            throw new AcessoNegadoException("Somente o dono do anúncio pode alterar as fotos dele");
        }
    }

    @Transactional
    public List<AnuncioFotoResponse> adicionarFotos(Long anuncioId, List<AnuncioFotoRequest> fotos) {
        Anuncio anuncio = anuncioRepository.findById(anuncioId)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Anuncio não encontrado"));

        exigirDonoDoAnuncio(anuncio, obterUsuarioAutenticado());

        List<AnuncioFoto> fotosParaSalvar = fotos.stream()
                .map(f -> {
                    AnuncioFoto foto = new AnuncioFoto();
                    foto.setUrl(f.getUrl());
                    foto.setOrdem(f.getOrdem());
                    foto.setTipoFoto(f.getTipoFoto());
                    foto.setAnuncio(anuncio);
                    return foto;

                })
                .toList();


        return anuncioFotoRepository.saveAll(fotosParaSalvar).stream()
                .map(AnuncioFotoResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AnuncioFotoResponse> listarFotosPorAnuncio(Long anuncioId, Pageable pageable) {
        return anuncioFotoRepository.findByAnuncioId(anuncioId, pageable)
                .map(AnuncioFotoResponse::new);
    }

    @Transactional
    public void deletarFoto(Long anuncioId, Long fotoId) {
        AnuncioFoto foto = anuncioFotoRepository.findById(fotoId)
                .orElseThrow(() -> new FotoNaoEncontradaException("Foto não encontrada"));

        if (!foto.getAnuncio().getId().equals(anuncioId)) {
            throw new FotoNaoEncontradaException("Foto não encontrada");
        }

        exigirDonoDoAnuncio(foto.getAnuncio(), obterUsuarioAutenticado());

        anuncioFotoRepository.delete(foto);
    }

}
