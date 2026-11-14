package com.josenetoo_dev.veiculos_api.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
@Entity
@Table(name = "solicitacao_privacidade")
@Getter @Setter
public class SolicitacaoPrivacidade {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch = FetchType.LAZY, optional = false)
 @JoinColumn(name = "titular_id", nullable = false) private Usuario titular;
 @Column(nullable = false, length = 30) private String tipo = "EXCLUSAO";
 @Column(nullable = false, length = 30) private String status = "SOLICITADA";
 @Column(name = "criado_em", nullable = false) private LocalDateTime criadoEm = LocalDateTime.now(ZoneOffset.UTC);
}
