package com.josenetoo_dev.veiculos_api.enums;
public enum StatusVerificacao {
    RASCUNHO, EM_ANALISE, INCONCLUSIVA, CORRECAO_NECESSARIA, APROVADA, REJEITADA, EXPIRADA;

    public boolean editavel() {
        return this == RASCUNHO || this == REJEITADA || this == CORRECAO_NECESSARIA || this == EXPIRADA;
    }

    public boolean aguardandoAnalise() { return this == EM_ANALISE || this == INCONCLUSIVA; }
}
