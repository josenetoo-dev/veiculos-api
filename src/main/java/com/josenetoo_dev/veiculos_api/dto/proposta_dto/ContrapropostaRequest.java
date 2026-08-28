package com.josenetoo_dev.veiculos_api.dto.proposta_dto;

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
public class ContrapropostaRequest {

    @NotNull(message = "O valor da contraproposta é obrigatório")
    private BigDecimal valor;

    private String descricao;
}
