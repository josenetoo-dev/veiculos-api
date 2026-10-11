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
    private final com.josenetoo_dev.veiculos_api.service.PasswordResetService resetService;

    @Operation(summary = "Solicitar recuperação de senha, sem revelar existência da conta")
    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> resetRequest(
            @Valid @RequestBody com.josenetoo_dev.veiculos_api.dto.auth.PasswordResetStartRequest request) {
        resetService.request(request);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Redefinir senha por código enviado ao endereço cadastrado")
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> resetConfirm(
            @Valid @RequestBody com.josenetoo_dev.veiculos_api.dto.auth.PasswordResetConfirmRequest request) {
        resetService.confirm(request);
        return ResponseEntity.noContent().build();
    }

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
