package com.josenetoo_dev.veiculos_api.controller;
import com.josenetoo_dev.veiculos_api.dto.usuario_dto.UsuarioResponse;
import com.josenetoo_dev.veiculos_api.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/v1/admin/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsuarioController {
    private final UsuarioService usuarios;
    @GetMapping public Page<UsuarioResponse> listar(Pageable pageable) { return usuarios.listarUsuarios(pageable); }
}
