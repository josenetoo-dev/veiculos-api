package com.josenetoo_dev.veiculos_api.dto.verificacao;

import com.josenetoo_dev.veiculos_api.enums.TipoEvidencia;
import com.josenetoo_dev.veiculos_api.model.EvidenciaVerificacao;
import java.time.LocalDateTime;

public record EvidenciaResponse(Long id, TipoEvidencia tipo, String tipoMidia,
                                Long tamanho, LocalDateTime criadoEm) {
    public static EvidenciaResponse from(EvidenciaVerificacao e) {
        return new EvidenciaResponse(e.getId(), e.getTipo(), e.getTipoMidia(), e.getTamanho(), e.getCriadoEm());
    }
}
