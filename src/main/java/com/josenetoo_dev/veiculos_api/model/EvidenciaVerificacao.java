package com.josenetoo_dev.veiculos_api.model;

import com.josenetoo_dev.veiculos_api.enums.TipoEvidencia;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Metadados. O arquivo criptografado está fora de diretórios servidos publicamente.
 * Nunca serializar esta entidade em resposta JSON.
 */
@Entity
@Table(name = "evidencia_verificacao", uniqueConstraints =
    @UniqueConstraint(name = "uq_evidencia_tipo", columnNames = {"verificacao_id", "tipo"}))
@Getter @Setter @NoArgsConstructor
public class EvidenciaVerificacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verificacao_id", nullable = false)
    private Verificacao verificacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    private TipoEvidencia tipo;

    @Column(name = "arquivo_chave", nullable = false, length = 36, unique = true)
    private String arquivoChave;

    @Column(name = "tipo_midia", nullable = false, length = 24)
    private String tipoMidia;

    @Column(name = "tamanho", nullable = false)
    private Long tamanho;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now(ZoneOffset.UTC);
}
