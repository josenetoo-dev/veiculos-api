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
    private final ImageValidator imageValidator;
    private final ImageStorage imageStorage;
    private final AnuncioService anuncioService;

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
    public List<AnuncioFotoResponse> uploadFotos(Long anuncioId, List<org.springframework.web.multipart.MultipartFile> arquivos) {
        Anuncio anuncio = anuncioRepository.findByIdForUpdate(anuncioId)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Anuncio não encontrado"));
        exigirDonoDoAnuncio(anuncio, obterUsuarioAutenticado());
        if ((anuncio.getStatus() == com.josenetoo_dev.veiculos_api.enums.StatusAnuncio.VENDIDO
                || anuncio.getStatus() == com.josenetoo_dev.veiculos_api.enums.StatusAnuncio.ARQUIVADO)) {
            throw new com.josenetoo_dev.veiculos_api.exception.ex.AnuncioIndisponivelException(
                    "Anúncio vendido não pode receber fotos");
        }
        long existing = anuncioFotoRepository.countByAnuncioId(anuncioId);
        if (arquivos == null || arquivos.isEmpty() || arquivos.size() > 10 || existing + arquivos.size() > 20) {
            throw new IllegalArgumentException("Limite de fotos excedido: 10 por lote e 20 por anúncio");
        }
        // Não há gravação antes de validar TODO o lote.
        var validated = arquivos.stream().map(imageValidator::validate).toList();
        var registros = new java.util.ArrayList<AnuncioFoto>();
        int ordem = anuncioFotoRepository.findByAnuncioId(anuncioId, Pageable.unpaged()).stream()
                .mapToInt(AnuncioFoto::getOrdem).max().orElse(-1) + 1;
        for (var image : validated) {
            AnuncioFoto foto = new AnuncioFoto();
            foto.setAnuncio(anuncio);
            foto.setUrl(imageStorage.save(image));
            foto.setOrdem(ordem++);
            foto.setTipoFoto(com.josenetoo_dev.veiculos_api.enums.TipoFoto.OUTRO);
            registros.add(foto);
        }
        anuncioService.reabrirRevisaoDeFotos(anuncio);
        return anuncioFotoRepository.saveAllAndFlush(registros).stream().map(AnuncioFotoResponse::new).toList();
    }

    @Transactional(readOnly = true)
    public Page<AnuncioFotoResponse> listarFotosPorAnuncio(Long anuncioId, Pageable pageable) {
        anuncioService.exigirVisibilidadePorId(anuncioId);
        return anuncioFotoRepository.findByAnuncioId(anuncioId, pageable)
                .map(AnuncioFotoResponse::new);
    }

    @Transactional
    public void deletarFoto(Long anuncioId, Long fotoId) {
        // Mesmo lock usado na aprovação administrativa e no upload.
        Anuncio anuncio = anuncioRepository.findByIdForUpdate(anuncioId)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Anuncio não encontrado"));
        exigirDonoDoAnuncio(anuncio, obterUsuarioAutenticado());
        AnuncioFoto foto = anuncioFotoRepository.findById(fotoId)
                .orElseThrow(() -> new FotoNaoEncontradaException("Foto não encontrada"));

        if (!foto.getAnuncio().getId().equals(anuncioId)) {
            throw new FotoNaoEncontradaException("Foto não encontrada");
        }

        if (anuncio.getStatus() == com.josenetoo_dev.veiculos_api.enums.StatusAnuncio.VENDIDO) {
            throw new com.josenetoo_dev.veiculos_api.exception.ex.AnuncioIndisponivelException(
                    "Anúncio vendido não pode ter fotos alteradas");
        }
        anuncioService.reabrirRevisaoDeFotos(anuncio);
        anuncioFotoRepository.delete(foto);
        imageStorage.deleteAfterCommit(foto.getUrl());
    }

}
