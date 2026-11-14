package com.josenetoo_dev.veiculos_api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "evento_assinatura")
@Getter @NoArgsConstructor
public class EventoAssinatura {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assinatura_id", nullable = false)
    private AssinaturaVendedor assinatura;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ator_id", nullable = false)
    private Usuario ator;
    @Column(nullable = false, length = 40)
    private String tipo;
    @Column(length = 500)
    private String detalhe;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now(ZoneOffset.UTC);

    public EventoAssinatura(AssinaturaVendedor assinatura, Usuario ator, String tipo, String detalhe) {
        this.assinatura = assinatura;
        this.ator = ator;
        this.tipo = tipo;
        this.detalhe = detalhe;
    }
}
