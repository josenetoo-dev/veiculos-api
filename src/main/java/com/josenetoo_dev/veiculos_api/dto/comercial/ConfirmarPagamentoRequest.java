package com.josenetoo_dev.veiculos_api.dto.comercial;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmarPagamentoRequest(@NotBlank @Size(max = 160) String referenciaExterna) {}
