package com.josenetoo_dev.veiculos_api.dto.comercial;

import com.josenetoo_dev.veiculos_api.model.VendaDeclarada;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record VendaDeclaradaResponse(Long id, Long anuncioId, Long vendedorId, String codigoPlano,
                                     String nomePlano, BigDecimal valorFinal, String moeda,
                                     BigDecimal comissaoPercentual, BigDecimal comissaoCalculada,
                                     LocalDate dataVendaDeclarada, LocalDateTime declaradoEm,
                                     String situacao) {
    public VendaDeclaradaResponse(VendaDeclarada v) {
        this(v.getId(), v.getAnuncio().getId(), v.getVendedor().getId(), v.getPlanoCodigoSnapshot(),
                v.getPlanoNomeSnapshot(), v.getValorFinal(), "BRL", v.getComissaoPercentual(),
                v.getComissaoCalculada(), v.getDataVendaDeclarada(), v.getDeclaradoEm(), v.getSituacao());
    }
}
