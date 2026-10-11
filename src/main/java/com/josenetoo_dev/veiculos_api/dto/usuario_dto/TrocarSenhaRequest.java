package com.josenetoo_dev.veiculos_api.dto.usuario_dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TrocarSenhaRequest {

    @NotBlank
    @Size(max = 72)
    private String senhaAtual;

    @NotBlank
    @com.josenetoo_dev.veiculos_api.validation.SafePassword
    private String novaSenha;

}
