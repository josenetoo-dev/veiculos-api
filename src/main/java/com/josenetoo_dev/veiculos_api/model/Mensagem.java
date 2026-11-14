package com.josenetoo_dev.veiculos_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensagem")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class Mensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String conteudo;

    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "excluida_em")
    private LocalDateTime excluidaEm;

    @Column(name = "excluida_por_id")
    private Long excluidaPorId;

    @Column(name = "arquivo_chave", length = 36, unique = true)
    private String arquivoChave;

    @Column(name = "arquivo_nome", length = 120)
    private String arquivoNome;

    @Column(name = "tipo_midia", length = 24)
    private String tipoMidia;

    @Column(name = "arquivo_tamanho")
    private Long arquivoTamanho;

    @ManyToOne
    @JoinColumn(name = "proposta_id", nullable = false)
    private Proposta proposta;

    @ManyToOne
    @JoinColumn(name = "remetente_id", nullable = false)
    private Usuario remetente;
}
