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
@ToString
public class Mensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String conteudo;

    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "proposta_id", nullable = false)
    private Proposta proposta;

    @ManyToOne
    @JoinColumn(name = "remetente_id", nullable = false)
    private Usuario remetente;
}
