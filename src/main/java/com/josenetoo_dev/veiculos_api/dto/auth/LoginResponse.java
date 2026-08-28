package com.josenetoo_dev.veiculos_api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String tipo = "Bearer";

    public LoginResponse(String token) {
        this.token = token;
    }

}
