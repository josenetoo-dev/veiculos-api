package com.josenetoo_dev.veiculos_api.dto.comercial;

import com.josenetoo_dev.veiculos_api.model.EventoAssinatura;
import java.time.LocalDateTime;

public record EventoAssinaturaResponse(String tipo, String detalhe, LocalDateTime criadoEm) {
    public EventoAssinaturaResponse(EventoAssinatura event) {
        this(event.getTipo(), event.getDetalhe(), event.getCriadoEm());
    }
}
