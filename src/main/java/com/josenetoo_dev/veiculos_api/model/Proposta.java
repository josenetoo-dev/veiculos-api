package com.josenetoo_dev.veiculos_api.model;

import com.josenetoo_dev.veiculos_api.enums.StatusProposta;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "proposta")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Proposta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(nullable = false)
    private String descricao;

    // VARCHAR explícito — ver o comentário equivalente em Anuncio.status.
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    @Enumerated(EnumType.STRING)
    private StatusProposta status;

    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    // Campos preenchidos quando o proprietário faz uma contraproposta
    @Column
    private BigDecimal contrapropostaValor;

    @Column
    private String contrapropostaDescricao;

    // Controla se já existe contraproposta — só é permitida uma por proposta
    @Column(nullable = false)
    private Boolean contrapropostaFeita = false;

    @ManyToOne
    @JoinColumn(name = "anunciante_id")
    private Anuncio anuncio;

    @ManyToOne
    @JoinColumn(name = "comprador_id")
    private Usuario comprador;
}