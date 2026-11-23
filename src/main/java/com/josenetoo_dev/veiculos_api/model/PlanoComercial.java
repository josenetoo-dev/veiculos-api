package com.josenetoo_dev.veiculos_api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.sql.Types;

@Entity
@Table(name = "plano_comercial")
@Getter @Setter @NoArgsConstructor
public class PlanoComercial {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 30)
    private String codigo;
    @Column(nullable = false, length = 80)
    private String nome;
    @Column(precision = 10, scale = 2)
    private BigDecimal mensalidade;
    /** Percentual, por exemplo 0.65 representa 0,65%. */
    @Column(name = "comissao_percentual", nullable = false, precision = 5, scale = 2)
    private BigDecimal comissaoPercentual;
    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(name = "beneficios_json", columnDefinition = "LONGTEXT", nullable = false)
    private String beneficiosJson = "[]";
    @Column(nullable = false, length = 10)
    private String periodicidade = "MENSAL";
    @Column(name = "limite_anuncios", nullable = false)
    private int limiteAnuncios = Integer.MAX_VALUE;
    @Column(name = "vigencia_dias", nullable = false)
    private int vigenciaDias = 30;
    @Column(nullable = false)
    private boolean ativo = true;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now(ZoneOffset.UTC);
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now(ZoneOffset.UTC);
}
