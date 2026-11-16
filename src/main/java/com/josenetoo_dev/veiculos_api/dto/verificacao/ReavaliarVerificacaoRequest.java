package com.josenetoo_dev.veiculos_api.dto.verificacao;

import com.josenetoo_dev.veiculos_api.enums.CategoriaResultadoVerificacao;
import jakarta.validation.constraints.*;

public record ReavaliarVerificacaoRequest(
        @NotBlank @Size(min = 10, max = 500) String motivo,
        @NotNull CategoriaResultadoVerificacao categoria) {}
