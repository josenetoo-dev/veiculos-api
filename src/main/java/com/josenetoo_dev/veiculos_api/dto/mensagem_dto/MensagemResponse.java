package com.josenetoo_dev.veiculos_api.dto.mensagem_dto;

import com.josenetoo_dev.veiculos_api.model.Mensagem;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MensagemResponse {

    private Long id;

    private Long propostaId;

    private Long remetenteId;

    private String remetenteNome;

    private String conteudo;

    private LocalDateTime criadoEm;

    public MensagemResponse(Mensagem mensagem) {
        this.id = mensagem.getId();
        this.propostaId = mensagem.getProposta().getId();
        this.remetenteId = mensagem.getRemetente().getId();
        this.remetenteNome = mensagem.getRemetente().getNome();
        this.conteudo = mensagem.getConteudo();
        this.criadoEm = mensagem.getCriadoEm();
    }
}
