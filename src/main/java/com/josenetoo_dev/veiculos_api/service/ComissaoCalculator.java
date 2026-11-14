package com.josenetoo_dev.veiculos_api.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ComissaoCalculator {
    public BigDecimal calculate(BigDecimal saleValue, BigDecimal commissionPercent) {
        if (saleValue == null || saleValue.signum() <= 0) {
            throw new IllegalArgumentException("O valor da venda deve ser positivo");
        }
        if (commissionPercent == null || commissionPercent.signum() < 0
                || commissionPercent.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("A taxa de comissão é inválida");
        }
        return saleValue.multiply(commissionPercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}
