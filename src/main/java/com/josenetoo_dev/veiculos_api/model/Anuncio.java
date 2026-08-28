package com.josenetoo_dev.veiculos_api.model;

import com.josenetoo_dev.veiculos_api.enums.Cambio;
import com.josenetoo_dev.veiculos_api.enums.Categoria;
import com.josenetoo_dev.veiculos_api.enums.StatusAnuncio;
import com.josenetoo_dev.veiculos_api.enums.TipoCombustivel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "anuncio")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Anuncio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String codigo;

    @Column(nullable = false)
    private String versao;

    @Column(nullable = false)
    private boolean destaque;

    private String laudoCautelar;

    @Column(nullable = false)
    private String documentacao;

    @Column(nullable = false)
    private String garantia;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private BigDecimal preco;

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false)
    private String modelo;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false)
    private int quilometragem;

    @Column(nullable = false)
    private String cor;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoCombustivel combustivel;

    @Column(nullable = false)
    private boolean segundaMao;

    // VARCHAR explícito: sem isso, o Hibernate cria um ENUM nativo no MySQL a partir
    // dos valores atuais — se um valor novo for adicionado ao enum Java depois, o
    // ddl-auto=update não amplia o ENUM do banco e toda escrita com o valor novo falha
    // com "Data truncated for column" (foi exatamente o que quebrou a contraproposta).
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    @Enumerated(EnumType.STRING)
    private StatusAnuncio status;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Cambio cambio;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Categoria categoria;

    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
