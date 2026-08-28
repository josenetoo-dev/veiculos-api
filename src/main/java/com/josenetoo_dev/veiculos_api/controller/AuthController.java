package com.josenetoo_dev.veiculos_api.controller;

import com.josenetoo_dev.veiculos_api.dto.auth.LoginRequest;
import com.josenetoo_dev.veiculos_api.dto.auth.LoginResponse;
import com.josenetoo_dev.veiculos_api.dto.auth.RegisterRequest;
import com.josenetoo_dev.veiculos_api.dto.usuario_dto.UsuarioResponse;
import com.josenetoo_dev.veiculos_api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Registrar novo usuario")
    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @Operation(summary = "Autenticar usuario")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity
                .ok(authService.login(request));
    }

}
