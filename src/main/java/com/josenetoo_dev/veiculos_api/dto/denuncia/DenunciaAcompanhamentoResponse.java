package com.josenetoo_dev.veiculos_api.dto.denuncia;

import com.josenetoo_dev.veiculos_api.model.Denuncia;
import java.time.LocalDateTime;

/** Protocolo do denunciante: sem justificativa interna, dados do revisor ou terceiros. */
public record DenunciaAcompanhamentoResponse(Long id, Long anuncioId, String estado,
                                            LocalDateTime recebidoEm, LocalDateTime atualizadoEm) {
    public static DenunciaAcompanhamentoResponse from(Denuncia d) {
        String estado = switch (d.getStatus()) {
            case ABERTA -> "RECEBIDA";
            case EM_ANALISE -> "EM_ANALISE";
            case CONFIRMADA -> "RESOLVIDA";
            case DESCARTADA, REVERTIDA -> "ENCERRADA_SEM_PROVIDENCIA";
        };
        return new DenunciaAcompanhamentoResponse(d.getId(),d.getAnuncio().getId(),estado,
                d.getCriadoEm(),d.getRevertidoEm()!=null?d.getRevertidoEm():d.getRevisadoEm());
    }
}
