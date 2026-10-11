package com.josenetoo_dev.veiculos_api.exception.ex;

public class EmailChangeUnavailableException extends RuntimeException {
    public EmailChangeUnavailableException() {
        super("Serviço de confirmação de e-mail indisponível");
    }
}
