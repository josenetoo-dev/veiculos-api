package com.josenetoo_dev.veiculos_api.dto.anuncio_dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejeitarAnuncioRequest(
        @NotBlank @Size(max = 500) String motivo
) {}
