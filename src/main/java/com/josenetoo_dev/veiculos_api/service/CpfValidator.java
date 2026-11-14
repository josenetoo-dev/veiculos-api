package com.josenetoo_dev.veiculos_api.service;

public final class CpfValidator {
    private CpfValidator() {}

    public static String normalizeAndValidate(String value) {
        if (value == null) throw new IllegalArgumentException("CPF inválido");
        String digits = value.replaceAll("[.\\-]", "");
        if (!digits.matches("\\d{11}") || digits.chars().distinct().count() == 1) {
            throw new IllegalArgumentException("CPF inválido");
        }
        if (digit(digits.substring(0, 9), 10) != Character.digit(digits.charAt(9), 10)
                || digit(digits.substring(0, 10), 11) != Character.digit(digits.charAt(10), 10)) {
            throw new IllegalArgumentException("CPF inválido");
        }
        return digits;
    }

    private static int digit(String base, int weight) {
        int sum = 0;
        for (int i = 0; i < base.length(); i++) sum += Character.digit(base.charAt(i), 10) * (weight - i);
        int remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }
}
