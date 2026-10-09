package com.josenetoo_dev.veiculos_api.dto.usuario_dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailChangeConfirmation(
        @NotBlank @Size(min = 32, max = 128) String token
) {}
