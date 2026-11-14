package com.josenetoo_dev.veiculos_api.dto.comercial;

import com.josenetoo_dev.veiculos_api.model.AceiteContrato;
import java.time.LocalDateTime;

public record AceiteContratoResponse(Long id, String versao, String hashAceito,
                                     String tipoAceite, boolean habilitaOperacaoComercial,
                                     LocalDateTime aceitoEm) {
    public AceiteContratoResponse(AceiteContrato a) {
        this(a.getId(), a.getContrato().getVersao(), a.getHashAceito(), a.getTipoAceite(),
                a.getContrato().isPublicado(), a.getAceitoEm());
    }
}
