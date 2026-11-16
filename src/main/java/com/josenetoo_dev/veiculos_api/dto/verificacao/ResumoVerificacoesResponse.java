package com.josenetoo_dev.veiculos_api.dto.verificacao;

import java.time.LocalDateTime;

/** Fatos públicos mínimos. Não concede nível de confiança ou comprovação de venda. */
public record ResumoVerificacoesResponse(AnaliseDocumental vendedor, AnaliseDocumental veiculo, String aviso) {
    public record AnaliseDocumental(boolean analisadaManualmente, LocalDateTime analisadaEm) {}
}
