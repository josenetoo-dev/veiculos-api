package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.enums.Role;
import com.josenetoo_dev.veiculos_api.enums.StatusUsuario;
import com.josenetoo_dev.veiculos_api.exception.ex.AcessoNegadoException;
import com.josenetoo_dev.veiculos_api.exception.ex.CredenciaisInvalidasException;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final UsuarioRepository usuarios;

    public Usuario current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new CredenciaisInvalidasException("Usuário não autenticado");
        }
        try {
            Usuario user = usuarios.findById(Long.valueOf(authentication.getName()))
                    .orElseThrow(() -> new CredenciaisInvalidasException("Usuário autenticado não encontrado"));
            if (!user.getStatus().podeAutenticar()) throw new CredenciaisInvalidasException("Conta indisponível");
            return user;
        } catch (NumberFormatException ex) {
            throw new CredenciaisInvalidasException("Credenciais inválidas");
        }
    }

    public Usuario admin() {
        Usuario user = current();
        if (user.getStatus() != StatusUsuario.ACTIVE || user.getRole() != Role.ADMIN) {
            throw new AcessoNegadoException("Administrador ativo obrigatório");
        }
        return user;
    }
}
