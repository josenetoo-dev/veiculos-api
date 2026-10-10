package com.josenetoo_dev.veiculos_api.dto.auth;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Always respond 202, even for unknown e-mail addresses. */
public record PasswordResetStartRequest(@NotBlank @Email @Size(max=254) String email) {}
