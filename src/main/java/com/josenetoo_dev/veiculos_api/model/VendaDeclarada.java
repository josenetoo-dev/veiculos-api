package com.josenetoo_dev.veiculos_api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "venda_declarada")
@Getter @Setter @NoArgsConstructor
public class VendaDeclarada {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anuncio_id", nullable = false, unique = true)
    private Anuncio anuncio;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private Usuario vendedor;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assinatura_id")
    private AssinaturaVendedor assinatura;
    @Column(name = "plano_codigo_snapshot", nullable = false, length = 30)
    private String planoCodigoSnapshot;
    @Column(name = "plano_nome_snapshot", nullable = false, length = 80)
    private String planoNomeSnapshot;
    @Column(name = "valor_final", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorFinal;
    @Column(name = "comissao_percentual", nullable = false, precision = 5, scale = 2)
    private BigDecimal comissaoPercentual;
    @Column(name = "comissao_calculada", nullable = false, precision = 15, scale = 2)
    private BigDecimal comissaoCalculada;
    @Column(name = "data_venda_declarada", nullable = false)
    private LocalDate dataVendaDeclarada;
    @Column(name = "declarado_em", nullable = false)
    private LocalDateTime declaradoEm = LocalDateTime.now(ZoneOffset.UTC);
    @Column(nullable = false, length = 40)
    private String situacao = "PENDENTE_CONFERENCIA_EXTERNA";
}
