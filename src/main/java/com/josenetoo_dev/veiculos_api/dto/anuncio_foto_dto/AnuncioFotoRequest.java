package com.josenetoo_dev.veiculos_api.dto.anuncio_foto_dto;

import com.josenetoo_dev.veiculos_api.enums.TipoFoto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AnuncioFotoRequest {

    @NotBlank
    private String url;

    @NotNull
    private int ordem;

    private TipoFoto tipoFoto;
}
