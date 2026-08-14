package com.josenetoo_dev.veiculos_api.dto.anuncio_dto;

import com.josenetoo_dev.veiculos_api.enums.Cambio;
import com.josenetoo_dev.veiculos_api.enums.Categoria;
import com.josenetoo_dev.veiculos_api.enums.StatusAnuncio;
import com.josenetoo_dev.veiculos_api.enums.TipoCombustivel;
import com.josenetoo_dev.veiculos_api.model.Anuncio;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class AnuncioResponse {

    private Long id;

    private String codigo;

    private String versao;

    private boolean destaque;

    private String laudoCautelar;

    private String documentacao;

    private String garantia;

    private String titulo;

    private String descricao;

    private BigDecimal preco;

    private String marca;

    private String modelo;

    private Integer ano;

    private int quilometragem;

    private String cor;

    private TipoCombustivel combustivel;

    private boolean segundaMao;

    private StatusAnuncio status;

    private Cambio cambio;

    private Categoria categoria;

    private LocalDateTime criadoEm = LocalDateTime.now();

    private Long usuarioId;
    private String usuarioNome;

    public AnuncioResponse(Anuncio anuncio) {
        this.id = anuncio.getId();
        this.codigo = anuncio.getCodigo();
        this.versao = anuncio.getVersao();
        this.destaque = anuncio.isDestaque();
        this.laudoCautelar = anuncio.getLaudoCautelar();
        this.documentacao = anuncio.getDocumentacao();
        this.garantia = anuncio.getGarantia();
        this.titulo = anuncio.getTitulo();
        this.descricao = anuncio.getDescricao();
        this.preco = anuncio.getPreco();
        this.marca = anuncio.getMarca();
        this.modelo = anuncio.getModelo();
        this.ano = anuncio.getAno();
        this.quilometragem = anuncio.getQuilometragem();
        this.cor = anuncio.getCor();
        this.combustivel = anuncio.getCombustivel();
        this.segundaMao = anuncio.isSegundaMao();
        this.status = anuncio.getStatus();
        this.cambio = anuncio.getCambio();
        this.categoria = anuncio.getCategoria();
        this.criadoEm = anuncio.getCriadoEm();
        this.usuarioId = anuncio.getUsuario().getId();
        this.usuarioNome = anuncio.getUsuario().getNome();
    }
}
