package com.josenetoo_dev.veiculos_api.model;

import com.josenetoo_dev.veiculos_api.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Uma solicitação por identidade de usuário ou por veículo cadastrado.
 * Aprovação MANUAL, não consulta oficial a SERPRO/SENATRAN.
 */
@Entity
@Table(name = "verificacao")
@Getter @Setter @NoArgsConstructor
public class Verificacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_identidade_id", unique = true)
    private Usuario usuarioIdentidade;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", unique = true)
    private Veiculo veiculo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private TipoVerificacao tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private StatusVerificacao status = StatusVerificacao.RASCUNHO;

    @Column(name = "motivo_rejeicao", length = 500)
    private String motivoRejeicao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now(ZoneOffset.UTC);

    @Column(name = "enviado_em")
    private LocalDateTime enviadoEm;

    @Column(name = "revisado_em")
    private LocalDateTime revisadoEm;

    @Column(name = "revisado_por_id")
    private Long revisadoPorId;

    public static Verificacao identidade(Usuario usuario) {
        Verificacao v = new Verificacao();
        v.solicitante = usuario;
        v.usuarioIdentidade = usuario;
        v.tipo = TipoVerificacao.IDENTIDADE;
        return v;
    }

    public static Verificacao veiculo(Usuario usuario, Veiculo veiculo) {
        Verificacao v = new Verificacao();
        v.solicitante = usuario;
        v.veiculo = veiculo;
        v.tipo = TipoVerificacao.VEICULO;
        return v;
    }
}
