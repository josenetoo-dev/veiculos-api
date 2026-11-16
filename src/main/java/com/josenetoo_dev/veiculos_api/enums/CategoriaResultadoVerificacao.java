package com.josenetoo_dev.veiculos_api.enums;

/** Classificação operacional privada; falha técnica nunca significa fraude. */
public enum CategoriaResultadoVerificacao {
    FALHA_TECNICA, DOCUMENTO_ILEGIVEL, INFORMACAO_INCONSISTENTE,
    VERIFICACAO_INCONCLUSIVA, SUSPEITA_FRAUDE, FRAUDE_CONFIRMADA, REVALIDACAO_NECESSARIA
}
