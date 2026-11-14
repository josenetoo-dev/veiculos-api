package com.josenetoo_dev.veiculos_api.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CpfValidatorTest {
    @Test void acceptsValidCpfInFormattedOrDigitsOnlyForm() {
        assertEquals("52998224725", CpfValidator.normalizeAndValidate("529.982.247-25"));
        assertEquals("52998224725", CpfValidator.normalizeAndValidate("52998224725"));
    }

    @Test void rejectsRepeatedDigitsAndIncorrectCheckDigits() {
        assertThrows(IllegalArgumentException.class, () -> CpfValidator.normalizeAndValidate("111.111.111-11"));
        assertThrows(IllegalArgumentException.class, () -> CpfValidator.normalizeAndValidate("529.982.247-24"));
    }
}
