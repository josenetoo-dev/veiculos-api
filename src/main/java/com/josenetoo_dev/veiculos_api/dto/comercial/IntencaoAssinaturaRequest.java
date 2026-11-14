package com.josenetoo_dev.veiculos_api.dto.comercial;

import jakarta.validation.constraints.NotBlank;

public record IntencaoAssinaturaRequest(@NotBlank String codigoPlano) {}
