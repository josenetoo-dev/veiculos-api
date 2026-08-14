package com.josenetoo_dev.veiculos_api.model;

import com.josenetoo_dev.veiculos_api.enums.TipoFoto;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "anuncio_foto")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AnuncioFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "anuncio_id", nullable = false)
    private Anuncio anuncio;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private int ordem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoFoto tipoFoto;
}
