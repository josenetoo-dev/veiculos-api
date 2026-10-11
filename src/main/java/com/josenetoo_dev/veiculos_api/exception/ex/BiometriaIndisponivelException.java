package com.josenetoo_dev.veiculos_api.exception.ex;

public class BiometriaIndisponivelException extends RuntimeException {
    public BiometriaIndisponivelException() {
        super("Serviço de verificação facial indisponível");
    }
}
