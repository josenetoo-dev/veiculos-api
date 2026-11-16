package com.josenetoo_dev.veiculos_api.dto.comercial;

import com.josenetoo_dev.veiculos_api.model.AssinaturaVendedor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AssinaturaResponse(Long id, String codigoPlano, String nomePlano, BigDecimal mensalidade,
                                 BigDecimal comissaoPercentual, String moeda, String periodicidade,
                                 String status, String situacaoFinanceira, LocalDateTime criadoEm,
                                 LocalDateTime inicioVigencia, LocalDateTime vencimentoEm,
                                 LocalDateTime canceladaEm, String referenciaPagamentoExterno, boolean renovacaoCancelada) {
    public AssinaturaResponse(AssinaturaVendedor a) {
        this(a.getId(), a.getPlanoCodigoSnapshot(), a.getPlanoNomeSnapshot(), a.getMensalidadeSnapshot(),
                a.getComissaoPercentualSnapshot(), "BRL", a.getPeriodicidadeSnapshot(), a.getStatus().name(),
                a.getSituacaoFinanceira(), a.getCriadoEm(), a.getInicioVigencia(), a.getVencimentoEm(),
                a.getCanceladaEm(), a.getReferenciaPagamentoExterno(), a.isRenovacaoCancelada());
    }
}
