package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.exception.ex.FotoNaoEncontradaException;
import com.josenetoo_dev.veiculos_api.model.AnuncioFoto;
import com.josenetoo_dev.veiculos_api.repository.AnuncioFotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FotoPublicaService {
    private final AnuncioFotoRepository fotos;
    private final AnuncioService anuncios;
    private final ImageStorage storage;

    public record Content(byte[] bytes, String mediaType) {}

    /**
     * Foto só pode ser baixada se estiver vinculada a um anúncio visível
     * para o solicitante. URLs diretas antigas também passam por esta regra.
     */
    @Transactional(readOnly = true)
    public Content read(String filename) {
        if (filename == null || !filename.matches(
                "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.(jpg|png)")) {
            throw new FotoNaoEncontradaException("Foto não encontrada");
        }
        AnuncioFoto photo=fotos.findFirstByUrlEndingWith("/uploads/fotos/" + filename)
                .orElseThrow(()->new FotoNaoEncontradaException("Foto não encontrada"));
        anuncios.exigirVisibilidadePorId(photo.getAnuncio().getId());
        byte[] bytes=storage.readAuthorized(filename);
        String mediaType=filename.endsWith(".png")?"image/png":"image/jpeg";
        return new Content(bytes,mediaType);
    }
}
