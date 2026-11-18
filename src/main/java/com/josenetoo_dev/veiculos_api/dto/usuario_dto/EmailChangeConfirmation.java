package com.josenetoo_dev.veiculos_api.dto.usuario_dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailChangeConfirmation(
        @NotBlank @Size(min = 6, max = 43) @jakarta.validation.constraints.Pattern(
        regexp = "(?:\\d{6}|[A-Za-z0-9_-]{43})",
        message = "Informe o código de 6 dígitos") String token
) {}
