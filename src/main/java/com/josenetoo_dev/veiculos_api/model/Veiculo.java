package com.josenetoo_dev.veiculos_api.model;

import com.josenetoo_dev.veiculos_api.enums.Cambio;
import com.josenetoo_dev.veiculos_api.enums.TipoCombustivel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dados descritivos do veículo, independentes do preço, anúncio e negociação.
 * cadastradoPor identifica quem enviou o cadastro — não é uma comprovação
 * de titularidade no RENAVAM/SENATRAN.
 */
@Entity
@Table(name = "veiculo")
@Getter
@Setter
@NoArgsConstructor
public class Veiculo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cadastrado_por_id")
    private Usuario cadastradoPor;

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false)
    private String modelo;

    @Column(nullable = false)
    private String versao;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false)
    private Integer quilometragem;

    @Column(nullable = false)
    private String cor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCombustivel combustivel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Cambio cambio;

    @Column(nullable = false)
    private boolean segundaMao;
}
