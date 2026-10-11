package com.josenetoo_dev.veiculos_api.dto.usuario_dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** O token só é entregue à caixa postal cadastrada, nunca em resposta da API. */
public record ConfirmarContatoEmailRequest(@NotBlank @Size(min = 32, max = 128) String token) {}
