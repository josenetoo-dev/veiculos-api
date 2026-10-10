package com.josenetoo_dev.veiculos_api.model;

import com.josenetoo_dev.veiculos_api.enums.StatusSessaoBiometria;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Apenas metadados necessários ao controle de sessão e auditoria.
 * Não guarda selfie, referência facial ou pontuações biométricas.
 */
@Entity
@Table(name = "sessao_biometria")
@Getter @Setter @NoArgsConstructor
public class SessaoBiometria {
    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verificacao_id", nullable = false)
    private Verificacao verificacao;

    @Column(name = "documento_evidencia_id", nullable = false)
    private Long documentoEvidenciaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    private StatusSessaoBiometria status = StatusSessaoBiometria.CRIADA;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now(ZoneOffset.UTC);

    @Column(name = "finalizado_em")
    private LocalDateTime finalizadoEm;

    @Column(name = "aceite_biometria_em", nullable = false)
    private LocalDateTime aceiteBiometriaEm = LocalDateTime.now(ZoneOffset.UTC);

    @Column(name = "versao_politica", nullable = false, length = 60)
    private String versaoPolitica;
}
