package com.josenetoo_dev.veiculos_api.dto.proposta_dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties("compradorId")
public class PropostaRequest {

    @NotNull
    @Positive
    @Digits(integer = 10, fraction = 2)
    private BigDecimal valor;

    @NotBlank
    @Size(max = 255)
    private String descricao;

    @NotNull
    @Positive
    private Long anuncioId;
}
