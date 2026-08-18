package com.josenetoo_dev.veiculos_api.dto.anuncio_foto_dto;

import com.josenetoo_dev.veiculos_api.enums.TipoFoto;
import com.josenetoo_dev.veiculos_api.model.AnuncioFoto;
import lombok.Getter;

@Getter
public class AnuncioFotoResponse {

    private Long id;

    private String url;

    private int ordem;

    private TipoFoto tipoFoto;

    private Long anuncioId;

    public AnuncioFotoResponse(AnuncioFoto anuncioFoto) {
        this.id = anuncioFoto.getId();
        this.url = anuncioFoto.getUrl();
        this.ordem = anuncioFoto.getOrdem();
        this.tipoFoto = anuncioFoto.getTipoFoto();
        this.anuncioId = anuncioFoto.getAnuncio().getId();
    }
}
