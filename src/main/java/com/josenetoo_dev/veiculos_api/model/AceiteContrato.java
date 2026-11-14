package com.josenetoo_dev.veiculos_api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "aceite_contrato", uniqueConstraints =
        @UniqueConstraint(name = "uq_aceite_usuario_contrato", columnNames = {"usuario_id", "contrato_id"}))
@Getter @NoArgsConstructor
public class AceiteContrato {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrato_id", nullable = false)
    private ContratoVersao contrato;
    @Column(name = "hash_aceito", nullable = false, length = 64)
    private String hashAceito;
    @Column(name = "tipo_aceite", nullable = false, length = 30)
    private String tipoAceite;
    @Column(name = "aceito_em", nullable = false)
    private LocalDateTime aceitoEm = LocalDateTime.now(ZoneOffset.UTC);
    @Column(name = "provedor_assinatura", length = 80)
    private String provedorAssinatura;
    @Column(name = "referencia_provedor", length = 160)
    private String referenciaProvedor;

    public AceiteContrato(Usuario usuario, ContratoVersao contrato, String hashAceito) {
        this.usuario = usuario;
        this.contrato = contrato;
        this.hashAceito = hashAceito;
        this.tipoAceite = contrato.isPublicado() ? "ACEITE_INTERNO" : "DEMONSTRATIVO";
    }
}
