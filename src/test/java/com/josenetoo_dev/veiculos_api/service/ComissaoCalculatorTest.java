package com.josenetoo_dev.veiculos_api.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComissaoCalculatorTest {
    private final ComissaoCalculator calculator = new ComissaoCalculator();

    @Test
    void calculatesBasicCommissionInBrlAndRoundsToCents() {
        assertEquals(new BigDecimal("65.00"), calculator.calculate(new BigDecimal("10000.00"), new BigDecimal("0.65")));
        assertEquals(new BigDecimal("0.01"), calculator.calculate(new BigDecimal("1.01"), new BigDecimal("0.65")));
    }

    @Test
    void calculatesPremiumCommissionInBrl() {
        assertEquals(new BigDecimal("50.00"), calculator.calculate(new BigDecimal("10000.00"), new BigDecimal("0.50")));
    }
}
