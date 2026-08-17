package com.josenetoo_dev.veiculos_api.dto.proposta_dto;

import com.josenetoo_dev.veiculos_api.enums.StatusProposta;
import com.josenetoo_dev.veiculos_api.model.Anuncio;
import com.josenetoo_dev.veiculos_api.model.Proposta;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class PropostaResponse {

    private Long id;

    private BigDecimal valor;

    private String descricao;

    private StatusProposta status;

    private LocalDateTime criadoEm = LocalDateTime.now();

    private Long anuncio;

    private Long comprador;

    public PropostaResponse(Proposta proposta) {
        this.id = proposta.getId();
        this.valor = proposta.getValor();
        this.descricao = proposta.getDescricao();
        this.status = proposta.getStatus();
        this.criadoEm = proposta.getCriadoEm();
        this.anuncio = proposta.getAnuncio().getId();
        this.comprador = proposta.getComprador().getId();
    }
}
