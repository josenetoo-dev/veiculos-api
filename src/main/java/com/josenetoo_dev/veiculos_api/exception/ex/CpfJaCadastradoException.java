package com.josenetoo_dev.veiculos_api.exception.ex;

public class CpfJaCadastradoException extends RuntimeException {
    public CpfJaCadastradoException() { super("CPF já vinculado a outra conta"); }
}
