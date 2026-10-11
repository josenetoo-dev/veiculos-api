package com.josenetoo_dev.veiculos_api.exception.ex;

public class BiometriaLimiteExcedidoException extends RuntimeException {
    public BiometriaLimiteExcedidoException() {
        super("Limite temporário de tentativas de biometria atingido");
    }
}
