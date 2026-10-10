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

    // O backend antigo não armazenava localização. Ambos opcionais apenas
    // para compatibilidade com anúncios legados, mas nunca parcialmente.
    @Size(max = 100)
    private String cidade;

    @Pattern(regexp = "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO",
        message = "UF deve ser uma sigla brasileira válida em letras maiúsculas")
    private String uf;

    @AssertTrue(message = "Informe cidade e UF juntas")
    public boolean isLocalizacaoConsistente() {
        boolean hasCity = cidade != null && !cidade.isBlank();
        boolean hasUf = uf != null && !uf.isBlank();
        return hasCity == hasUf;
    }

    @NotNull
    private TipoCombustivel combustivel;

    private boolean segundaMao;

    @NotNull
    private Cambio cambio;

    @NotNull
    private Categoria categoria;
}