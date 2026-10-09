package com.josenetoo_dev.veiculos_api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    @Email
    @NotBlank
    @Size(max = 254)
    private String email;

    @NotBlank
    @Size(max = 72)
    private String senha;

}
