package com.josenetoo_dev.veiculos_api.model;

import com.josenetoo_dev.veiculos_api.enums.StatusVerificacao;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Trilha de auditoria da revisão manual; não armazena imagens nem dados do documento. */
@Entity
@Table(name = "evento_verificacao")
@Getter @NoArgsConstructor
public class EventoVerificacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "verificacao_id", nullable = false)
    private Long verificacaoId;

    @Column(name = "revisor_id", nullable = false)
    private Long revisorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, columnDefinition = "VARCHAR(20)")
    private StatusVerificacao resultado;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now(ZoneOffset.UTC);

    public EventoVerificacao(Long verificacaoId, Long revisorId,
                             StatusVerificacao resultado, String motivo) {
        this.verificacaoId = verificacaoId;
        this.revisorId = revisorId;
        this.resultado = resultado;
        this.motivo = motivo;
    }
}
