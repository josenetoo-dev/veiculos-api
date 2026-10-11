package com.josenetoo_dev.veiculos_api.dto.denuncia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DecisaoDenunciaRequest(@NotBlank @Size(max=500) String motivo) {}
