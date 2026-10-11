package com.josenetoo_dev.veiculos_api.exception.ex;

/** Permite persistir o contador anti-bruteforce mesmo ao retornar 400. */
public class InvalidPasswordResetTokenException extends IllegalArgumentException {
    public InvalidPasswordResetTokenException() { super("Código inválido ou expirado"); }
}
