package com.josenetoo_dev.veiculos_api.model;

import com.josenetoo_dev.veiculos_api.enums.RespostaHistorico;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Respostas fornecidas pelo anunciante, sem validação independente pela plataforma.
 * Colunas nulas são permitidas no banco para não reescrever anúncios legados.
 */
@Embeddable
@Getter @Setter @NoArgsConstructor
public class HistoricoVeiculoDeclarado {
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "historico_colisao", length = 20)
    private RespostaHistorico colisao;

    @Size(max = 500)
    @Column(name = "historico_detalhes_colisao", length = 500)
    private String detalhesColisao;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "historico_repintura", length = 20)
    private RespostaHistorico repintura;

    @Size(max = 500)
    @Column(name = "historico_detalhes_repintura", length = 500)
    private String detalhesRepintura;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "historico_pecas_acidente", length = 20)
    private RespostaHistorico pecasAcidente;

    @Size(max = 500)
    @Column(name = "historico_detalhes_pecas_acidente", length = 500)
    private String detalhesPecasAcidente;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "historico_leilao", length = 20)
    private RespostaHistorico leilao;

    @Size(max = 500)
    @Column(name = "historico_detalhes_leilao", length = 500)
    private String detalhesLeilao;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "historico_alagamento", length = 20)
    private RespostaHistorico alagamento;

    @Size(max = 500)
    @Column(name = "historico_detalhes_alagamento", length = 500)
    private String detalhesAlagamento;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "historico_problema_mecanico", length = 20)
    private RespostaHistorico problemaMecanico;

    @Size(max = 500)
    @Column(name = "historico_detalhes_problema_mecanico", length = 500)
    private String detalhesProblemaMecanico;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "historico_sinistro", length = 20)
    private RespostaHistorico sinistro;

    @Size(max = 500)
    @Column(name = "historico_detalhes_sinistro", length = 500)
    private String detalhesSinistro;

}
