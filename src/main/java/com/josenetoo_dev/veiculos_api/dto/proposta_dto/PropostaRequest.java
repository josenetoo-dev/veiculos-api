package com.josenetoo_dev.veiculos_api.dto.proposta_dto;

import com.josenetoo_dev.veiculos_api.enums.StatusProposta;
import jakarta.validation.constraints.NotBlank;
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
public class PropostaRequest {

    @NotNull
    private BigDecimal valor;

    @NotBlank
    private String descricao;

    @NotNull
    private Long anuncioId;

    @NotNull
    private Long compradorId;

    private StatusProposta statusProposta;
}
