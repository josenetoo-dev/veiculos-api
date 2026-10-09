package com.josenetoo_dev.veiculos_api.dto.usuario_dto;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import java.time.LocalDateTime;
public record UsuarioPublicResponse(Long id, String nome, LocalDateTime criadoEm) {
    public UsuarioPublicResponse(Usuario u) { this(u.getId(), u.getNome(), u.getCriadoEm()); }
}
