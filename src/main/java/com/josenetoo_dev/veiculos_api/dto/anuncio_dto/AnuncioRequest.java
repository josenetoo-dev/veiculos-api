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
    @Size(max = 255)
    private String versao;

    @Size(max = 255)
    private String laudoCautelar;

    @NotBlank
    @Size(max = 255)
    private String documentacao;

    @NotBlank
    @Size(max = 255)
    private String garantia;

    @NotBlank
    @Size(max = 255)
    private String titulo;

    @NotBlank
    @Size(max = 255)
    private String descricao;

    @NotNull
    @Positive
    @Digits(integer = 10, fraction = 2)
    private BigDecimal preco;

    @NotBlank
    @Size(max = 255)
    private String marca;

    @NotBlank
    @Size(max = 255)
    private String modelo;

    @NotNull
    @Min(1900)
    private Integer ano;

    @NotNull
    @PositiveOrZero
    private Integer quilometragem;

    @NotBlank
    @Size(max = 255)
    private String cor;

    @NotNull
    private TipoCombustivel combustivel;

    private boolean segundaMao;

    @NotNull
    private Cambio cambio;

    @NotNull
    private Categoria categoria;
}