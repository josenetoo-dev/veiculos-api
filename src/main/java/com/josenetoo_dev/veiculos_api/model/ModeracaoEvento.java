package com.josenetoo_dev.veiculos_api.model;

import com.josenetoo_dev.veiculos_api.enums.StatusAnuncio;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Histórico imutável na API de decisões de moderação, inclusive após exclusão de anúncio. */
@Entity
@Table(name = "moderacao_evento")
@Getter
@NoArgsConstructor
public class ModeracaoEvento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anuncio_id", nullable = false)
    private Long anuncioId;

    @Column(name = "revisor_id", nullable = false)
    private Long revisorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private StatusAnuncio statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private StatusAnuncio statusNovo;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    public ModeracaoEvento(Long anuncioId, Long revisorId, StatusAnuncio anterior,
                           StatusAnuncio novo, String motivo) {
        this.anuncioId = anuncioId;
        this.revisorId = revisorId;
        this.statusAnterior = anterior;
        this.statusNovo = novo;
        this.motivo = motivo;
        this.criadoEm = LocalDateTime.now(ZoneOffset.UTC);
    }
}
