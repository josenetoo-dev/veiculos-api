package com.josenetoo_dev.veiculos_api.dto.verificacao;

import com.josenetoo_dev.veiculos_api.enums.*;
import com.josenetoo_dev.veiculos_api.model.Verificacao;
import java.time.LocalDateTime;

/** Nunca inclui documento, chave de storage ou dados pessoais privados. */
public record VerificacaoResponse(
        Long id, TipoVerificacao tipo, StatusVerificacao status,
        Long veiculoId, String motivoRejeicao,
        LocalDateTime enviadoEm, LocalDateTime revisadoEm
) {
    public static VerificacaoResponse from(Verificacao v) {
        return new VerificacaoResponse(v.getId(), v.getTipo(), v.getStatus(),
                v.getVeiculo() == null ? null : v.getVeiculo().getId(),
                v.getMotivoRejeicao(), v.getEnviadoEm(), v.getRevisadoEm());
    }
}
