package com.josenetoo_dev.veiculos_api.dto.verificacao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DispensarBiometriaRequest(@NotBlank @Size(min=10,max=500) String motivo) {}
