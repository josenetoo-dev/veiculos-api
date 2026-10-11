package com.josenetoo_dev.veiculos_api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank
    @Size(max = 120)
    private String nome;

    @Email
    @NotBlank
    @Size(max = 254)
    private String email;

    @NotBlank
    @com.josenetoo_dev.veiculos_api.validation.SafePassword
    private String senha;

    @Pattern(regexp = "\\d{10,11}", message = "Telefone invalido")
    @NotBlank
    private String telefone;

}
