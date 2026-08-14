package com.josenetoo_dev.veiculos_api.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErroResponse {

    private String mensagem;
    private int status;

}
