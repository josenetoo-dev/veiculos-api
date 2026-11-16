package com.josenetoo_dev.veiculos_api.exception.ex;

public class EmailVerificationCodeException extends IllegalArgumentException {
    public EmailVerificationCodeException() {
        super("Código inválido ou expirado. Solicite outro código se necessário.");
    }
}
