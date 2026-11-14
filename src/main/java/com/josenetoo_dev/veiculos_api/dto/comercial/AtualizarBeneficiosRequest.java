package com.josenetoo_dev.veiculos_api.dto.comercial;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AtualizarBeneficiosRequest(@Size(max = 20) List<@NotBlank @Size(max = 120) String> beneficios) {}
