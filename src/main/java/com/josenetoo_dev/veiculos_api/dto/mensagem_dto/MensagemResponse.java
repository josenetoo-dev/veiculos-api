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

    private boolean excluida;

    private String arquivoNome;
    private String tipoMidia;
    private Long arquivoTamanho;
    private boolean anexoDisponivel;

    public MensagemResponse(Mensagem mensagem) {
        this.id = mensagem.getId();
        this.propostaId = mensagem.getProposta().getId();
        this.remetenteId = mensagem.getRemetente().getId();
        this.remetenteNome = mensagem.getRemetente().getNome();
        this.excluida = mensagem.getExcluidaEm() != null;
        this.conteudo = excluida ? null : mensagem.getConteudo();
        this.arquivoNome = excluida ? null : mensagem.getArquivoNome();
        this.tipoMidia = excluida ? null : mensagem.getTipoMidia();
        this.arquivoTamanho = excluida ? null : mensagem.getArquivoTamanho();
        this.anexoDisponivel = !excluida && mensagem.getArquivoChave() != null;
        this.criadoEm = mensagem.getCriadoEm();
    }
}
