package com.josenetoo_dev.veiculos_api.model;

import com.josenetoo_dev.veiculos_api.enums.StatusAssinatura;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "assinatura_vendedor")
@Getter @Setter @NoArgsConstructor
public class AssinaturaVendedor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private Usuario vendedor;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plano_id", nullable = false)
    private PlanoComercial plano;
    @Column(name = "plano_codigo_snapshot", nullable = false, length = 30)
    private String planoCodigoSnapshot;
    @Column(name = "plano_nome_snapshot", nullable = false, length = 80)
    private String planoNomeSnapshot;
    @Column(name = "mensalidade_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal mensalidadeSnapshot;
    @Column(name = "comissao_percentual_snapshot", nullable = false, precision = 5, scale = 2)
    private BigDecimal comissaoPercentualSnapshot;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private StatusAssinatura status = StatusAssinatura.INTENCAO;
    @Column(name = "situacao_financeira", nullable = false, length = 30)
    private String situacaoFinanceira = "NAO_PAGO";
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now(ZoneOffset.UTC);
    @Column(name = "inicio_vigencia")
    private LocalDateTime inicioVigencia;
    @Column(name = "vencimento_em")
    private LocalDateTime vencimentoEm;
    @Column(name = "cancelada_em")
    private LocalDateTime canceladaEm;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmada_por_id")
    private Usuario confirmadaPor;
    @Column(name = "referencia_pagamento_externo", length = 160)
    private String referenciaPagamentoExterno;

    @Column(name = "periodicidade_snapshot", nullable = false, length = 10)
    private String periodicidadeSnapshot = "MENSAL";
    @Column(name = "limite_anuncios_snapshot", nullable = false)
    private int limiteAnunciosSnapshot = Integer.MAX_VALUE;
    @Column(name = "vigencia_dias_snapshot", nullable = false)
    private int vigenciaDiasSnapshot = 30;
    @Column(name = "renovacao_cancelada", nullable = false)
    private boolean renovacaoCancelada = false;

    public void snapshot(PlanoComercial selected) {
        periodicidadeSnapshot = selected.getPeriodicidade();
        limiteAnunciosSnapshot = selected.getLimiteAnuncios();
        vigenciaDiasSnapshot = selected.getVigenciaDias();
        plano = selected;
        planoCodigoSnapshot = selected.getCodigo();
        planoNomeSnapshot = selected.getNome();
        mensalidadeSnapshot = selected.getMensalidade();
        comissaoPercentualSnapshot = selected.getComissaoPercentual();
    }
}
