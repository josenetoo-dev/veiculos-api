package com.josenetoo_dev.veiculos_api.dto.proposta_dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
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

    private UUID envioId;

    public PropostaRequest(BigDecimal valor, String descricao, Long anuncioId) {
        this.valor = valor;
        this.descricao = descricao;
        this.anuncioId = anuncioId;
    }
}
