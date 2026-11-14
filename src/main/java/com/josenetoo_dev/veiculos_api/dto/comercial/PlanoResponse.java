package com.josenetoo_dev.veiculos_api.dto.comercial;

import com.josenetoo_dev.veiculos_api.model.PlanoComercial;
import java.math.BigDecimal;

public record PlanoResponse(String codigo, String nome, BigDecimal mensalidade, String moeda,
                            String periodicidade, BigDecimal comissaoPercentual, String beneficiosJson) {
    public PlanoResponse(PlanoComercial plano) {
        this(plano.getCodigo(), plano.getNome(), plano.getMensalidade(), "BRL", "MENSAL",
                plano.getComissaoPercentual(), plano.getBeneficiosJson());
    }
}
