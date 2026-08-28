package com.josenetoo_dev.veiculos_api.dto.usuario_dto;

import jakarta.validation.constraints.NotBlank;
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
    private String senhaAtual;

    @NotBlank
    private String novaSenha;

}
