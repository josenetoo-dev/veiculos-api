package com.josenetoo_dev.veiculos_api.dto.denuncia;

import com.josenetoo_dev.veiculos_api.enums.*;
import com.josenetoo_dev.veiculos_api.model.Denuncia;
import java.time.LocalDateTime;

/** Não expõe dados de contato do denunciante nem do anunciante. */
public record DenunciaResponse(
    Long id, Long anuncioId, CategoriaDenuncia categoria, String relato, StatusDenuncia status,
    String motivoDecisao, LocalDateTime criadoEm, LocalDateTime revisadoEm
) {
    public static DenunciaResponse from(Denuncia d) {
        return new DenunciaResponse(d.getId(), d.getAnuncio().getId(), d.getCategoria(),
            d.getRelato(), d.getStatus(), d.getMotivoDecisao(), d.getCriadoEm(), d.getRevisadoEm());
    }
}
