package com.josenetoo_dev.veiculos_api.dto.verificacao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejeitarVerificacaoRequest(@NotBlank @Size(max = 500) String motivo) {}
