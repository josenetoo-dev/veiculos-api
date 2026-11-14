package com.josenetoo_dev.veiculos_api.dto.comercial;

import com.josenetoo_dev.veiculos_api.model.ContratoVersao;
import java.time.LocalDateTime;

public record ContratoResponse(Long id, String versao, String titulo, String conteudo,
                               String hashSha256, boolean publicado, boolean demonstrativo,
                               LocalDateTime criadoEm) {
    public ContratoResponse(ContratoVersao c) {
        this(c.getId(), c.getVersao(), c.getTitulo(), c.getConteudo(), c.getHashSha256(),
                c.isPublicado(), !c.isPublicado(), c.getCriadoEm());
    }
}
