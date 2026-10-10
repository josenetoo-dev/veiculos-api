package com.josenetoo_dev.veiculos_api.dto.verificacao;

import com.josenetoo_dev.veiculos_api.enums.StatusSessaoBiometria;
import com.josenetoo_dev.veiculos_api.model.SessaoBiometria;
import java.time.LocalDateTime;

/** Não contém dados faciais ou confiança de modelo. */
public record SessaoBiometriaResponse(
    String sessionId, StatusSessaoBiometria status,
    LocalDateTime criadoEm, LocalDateTime finalizadoEm
) {
    public static SessaoBiometriaResponse from(SessaoBiometria item) {
        return new SessaoBiometriaResponse(item.getId(),item.getStatus(),
                item.getCriadoEm(),item.getFinalizadoEm());
    }
}
