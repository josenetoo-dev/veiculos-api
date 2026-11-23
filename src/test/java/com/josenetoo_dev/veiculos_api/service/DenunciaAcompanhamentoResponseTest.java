package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.denuncia.DenunciaAcompanhamentoResponse;
import com.josenetoo_dev.veiculos_api.enums.StatusDenuncia;
import com.josenetoo_dev.veiculos_api.model.Anuncio;
import com.josenetoo_dev.veiculos_api.model.Denuncia;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DenunciaAcompanhamentoResponseTest {
    @Test
    void mapsInternalStatesWithoutLeakingDecisionOrReviewer() {
        var report = new Denuncia();
        report.setId(8L);
        var ad = new Anuncio();
        ad.setId(7L);
        report.setAnuncio(ad);
        report.setMotivoDecisao("Detalhes internos não devem aparecer");
        report.setRevisadoPorId(99L);
        assertEquals("RECEBIDA", DenunciaAcompanhamentoResponse.from(report).estado());
        report.setStatus(StatusDenuncia.EM_ANALISE);
        assertEquals("EM_ANALISE", DenunciaAcompanhamentoResponse.from(report).estado());
        report.setStatus(StatusDenuncia.CONFIRMADA);
        assertEquals("RESOLVIDA", DenunciaAcompanhamentoResponse.from(report).estado());
        report.setStatus(StatusDenuncia.DESCARTADA);
        assertEquals("ENCERRADA_SEM_PROVIDENCIA", DenunciaAcompanhamentoResponse.from(report).estado());
        assertEquals(5, DenunciaAcompanhamentoResponse.class.getRecordComponents().length);
    }
}
