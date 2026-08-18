package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.anuncio_foto_dto.AnuncioFotoRequest;
import com.josenetoo_dev.veiculos_api.dto.anuncio_foto_dto.AnuncioFotoResponse;
import com.josenetoo_dev.veiculos_api.exception.ex.AnuncioNaoEncontradoException;
import com.josenetoo_dev.veiculos_api.model.Anuncio;
import com.josenetoo_dev.veiculos_api.model.AnuncioFoto;
import com.josenetoo_dev.veiculos_api.exception.ex.FotoNaoEncontradaException;
import com.josenetoo_dev.veiculos_api.repository.AnuncioFotoRepository;
import com.josenetoo_dev.veiculos_api.repository.AnuncioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnuncioFotoService {

    private final AnuncioFotoRepository anuncioFotoRepository;
    private final AnuncioRepository anuncioRepository;

    @Transactional
    public List<AnuncioFotoResponse> adicionarFotos(Long anuncioId, List<AnuncioFotoRequest> fotos) {
        Anuncio anuncio = anuncioRepository.findById(anuncioId)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Anuncio não encontrado"));

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
    public void deletarFoto(Long fotoId) {
        AnuncioFoto foto = anuncioFotoRepository.findById(fotoId)
                .orElseThrow(() -> new FotoNaoEncontradaException("Foto não encontrada"));
        anuncioFotoRepository.delete(foto);
    }

}
