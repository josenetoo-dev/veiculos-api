package com.josenetoo_dev.veiculos_api.dto.comercial;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DadosVendedorRequest(@NotBlank @Size(min = 3, max = 160) String nomeCompleto,
                                  @NotBlank @Size(max = 14) String cpf) {}
