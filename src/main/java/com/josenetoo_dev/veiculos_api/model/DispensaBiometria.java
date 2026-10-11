package com.josenetoo_dev.veiculos_api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Dispensa excepcional documentada, sem falsear a aprovação técnica.
 * Registro imutável no fluxo da API; a identidade continua sujeita a humano.
 */
@Entity
@Table(name="dispensa_biometria",
    uniqueConstraints=@UniqueConstraint(name="uq_dispensa_documento",
        columnNames={"verificacao_id","documento_evidencia_id"}))
@Getter @NoArgsConstructor
public class DispensaBiometria {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="verificacao_id",nullable=false)
    private Verificacao verificacao;

    @Column(name="documento_evidencia_id",nullable=false)
    private Long documentoEvidenciaId;

    @Column(name="revisor_id",nullable=false)
    private Long revisorId;

    @Column(nullable=false,length=500)
    private String motivo;

    @Column(name="criado_em",nullable=false)
    private LocalDateTime criadoEm=LocalDateTime.now(ZoneOffset.UTC);

    public DispensaBiometria(Verificacao verification, Long documentId,
                             Long reviewerId, String reason) {
        verificacao=verification;
        documentoEvidenciaId=documentId;
        revisorId=reviewerId;
        motivo=reason;
    }
}
