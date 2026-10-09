package com.josenetoo_dev.veiculos_api.exception.ex;

public class EmailChangeThrottledException extends RuntimeException {
    public EmailChangeThrottledException() {
        super("Solicitação de código muito frequente");
    }
}
