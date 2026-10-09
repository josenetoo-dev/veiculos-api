package com.josenetoo_dev.veiculos_api.dto.proposta_dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContrapropostaRequest {

    @NotNull(message = "O valor da contraproposta é obrigatório")
    @Positive
    @Digits(integer = 10, fraction = 2)
    private BigDecimal valor;

    @NotBlank
    @Size(max = 255)
    private String descricao;
}
