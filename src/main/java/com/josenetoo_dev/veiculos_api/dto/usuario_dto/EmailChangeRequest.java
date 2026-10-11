package com.josenetoo_dev.veiculos_api.dto.usuario_dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailChangeRequest(
        @NotBlank @Email @Size(max = 254) String newEmail,
        @NotBlank String currentPassword
) {}
