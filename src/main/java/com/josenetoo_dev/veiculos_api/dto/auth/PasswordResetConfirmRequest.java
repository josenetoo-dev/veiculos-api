package com.josenetoo_dev.veiculos_api.dto.auth;
import jakarta.validation.constraints.*;
import com.josenetoo_dev.veiculos_api.validation.SafePassword;

public record PasswordResetConfirmRequest(
    @NotBlank @Email @Size(max=254) String email,
    @NotBlank @Size(min=32,max=128) String token,
    @NotBlank @SafePassword String newPassword
) {}
