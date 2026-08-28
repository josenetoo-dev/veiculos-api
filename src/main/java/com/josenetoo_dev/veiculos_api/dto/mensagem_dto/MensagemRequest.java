package com.josenetoo_dev.veiculos_api.dto.mensagem_dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MensagemRequest {

    @NotBlank
    private String conteudo;
}
