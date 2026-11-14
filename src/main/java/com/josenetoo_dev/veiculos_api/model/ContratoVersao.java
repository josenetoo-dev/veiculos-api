package com.josenetoo_dev.veiculos_api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.sql.Types;

@Entity
@Table(name = "contrato_versao")
@Getter @Setter @NoArgsConstructor
public class ContratoVersao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 40)
    private String versao;
    @Column(nullable = false, length = 160)
    private String titulo;
    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String conteudo;
    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;
    @Column(nullable = false)
    private boolean publicado;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now(ZoneOffset.UTC);
}
