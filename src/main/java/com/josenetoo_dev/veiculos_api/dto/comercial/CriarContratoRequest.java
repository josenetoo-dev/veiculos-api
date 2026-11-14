package com.josenetoo_dev.veiculos_api.dto.comercial;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarContratoRequest(@NotBlank @Size(max = 40) String versao,
                                   @NotBlank @Size(max = 160) String titulo,
                                   @NotBlank @Size(max = 50000) String conteudo) {}
