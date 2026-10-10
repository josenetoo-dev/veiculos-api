package com.josenetoo_dev.veiculos_api.dto.verificacao;

import jakarta.validation.constraints.AssertTrue;

public record IniciarBiometriaRequest(
    @AssertTrue(message = "Aceite explícito necessário") boolean aceiteBiometria
) {}
