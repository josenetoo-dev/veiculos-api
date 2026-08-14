package com.josenetoo_dev.veiculos_api.dto.usuario_dto;

import com.josenetoo_dev.veiculos_api.model.Usuario;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UsuarioResponse {

    private Long id;

    private String nome;

    private String email;

    private String telefone;

    private LocalDateTime criadoEm = LocalDateTime.now();

    public UsuarioResponse(Usuario usuario) {
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.email = usuario.getEmail();
        this.telefone = usuario.getTelefone();
        this.criadoEm = usuario.getCriadoEm();
    }

}
