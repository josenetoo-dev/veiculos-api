package com.josenetoo_dev.veiculos_api.dto.anuncio_dto;

import com.josenetoo_dev.veiculos_api.enums.Cambio;
import com.josenetoo_dev.veiculos_api.enums.Categoria;
import com.josenetoo_dev.veiculos_api.enums.TipoCombustivel;
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
public class AnuncioRequest {

    @NotBlank
    private String versao;

    private String laudoCautelar;

    @NotBlank
    private String documentacao;

    @NotBlank
    private String garantia;

    @NotBlank
    private String titulo;

    @NotBlank
    private String descricao;

    @NotNull
    @Positive
    private BigDecimal preco;

    @NotBlank
    private String marca;

    @NotBlank
    private String modelo;

    @NotNull
    @Min(1900)
    private Integer ano;

    @NotNull
    @PositiveOrZero
    private Integer quilometragem;

    @NotBlank
    private String cor;

    @NotNull
    private TipoCombustivel combustivel;

    private boolean segundaMao;

    @NotNull
    private Cambio cambio;

    @NotNull
    private Categoria categoria;
}