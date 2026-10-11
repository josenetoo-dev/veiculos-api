package com.josenetoo_dev.veiculos_api.model;

import com.josenetoo_dev.veiculos_api.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "denuncia", uniqueConstraints =
    @UniqueConstraint(name="uq_denuncia_anuncio_usuario", columnNames={"anuncio_id","denunciante_id"}))
@Getter @Setter @NoArgsConstructor
public class Denuncia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anuncio_id", nullable = false)
    private Anuncio anuncio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "denunciante_id", nullable = false)
    private Usuario denunciante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition="VARCHAR(30)")
    private CategoriaDenuncia categoria;

    @Column(nullable = false, length = 1000)
    private String relato;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition="VARCHAR(20)")
    private StatusDenuncia status = StatusDenuncia.ABERTA;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now(ZoneOffset.UTC);

    @Column(name = "revisado_em")
    private LocalDateTime revisadoEm;

    @Column(name = "revisado_por_id")
    private Long revisadoPorId;

    @Column(name = "motivo_decisao", length=500)
    private String motivoDecisao;

    @Column(name = "revertido_em")
    private LocalDateTime revertidoEm;

    @Column(name = "revertido_por_id")
    private Long revertidoPorId;

    @Column(name = "motivo_reversao", length = 500)
    private String motivoReversao;
}
