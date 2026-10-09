package com.josenetoo_dev.veiculos_api.controller;

import com.josenetoo_dev.veiculos_api.dto.usuario_dto.TrocarSenhaRequest;
import com.josenetoo_dev.veiculos_api.dto.usuario_dto.UsuarioRequest;
import com.josenetoo_dev.veiculos_api.dto.usuario_dto.UsuarioResponse;
import com.josenetoo_dev.veiculos_api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/usuario")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;

    private final com.josenetoo_dev.veiculos_api.service.EmailChangeService emailChangeService;
    private final com.josenetoo_dev.veiculos_api.service.ContatoEmailService contatoEmailService;

    @Operation(summary = "Enviar código ao e-mail da conta para ativar o contato")
    @PostMapping("/me/contato/email/solicitar")
    public ResponseEntity<Void> solicitarContatoEmail() {
        contatoEmailService.solicitar();
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Confirmar código recebido no e-mail cadastrado")
    @PostMapping("/me/contato/email/confirmar")
    public ResponseEntity<Void> confirmarContatoEmail(
            @Valid @RequestBody com.josenetoo_dev.veiculos_api.dto.usuario_dto.ConfirmarContatoEmailRequest request) {
        contatoEmailService.confirmar(request);
        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "Solicitar troca de e-mail após confirmar a senha atual")
    @PostMapping("/me/email-change")
    public ResponseEntity<Void> solicitarTrocaEmail(
            @Valid @RequestBody com.josenetoo_dev.veiculos_api.dto.usuario_dto.EmailChangeRequest request) {
        emailChangeService.requestChange(request);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Confirmar troca usando código enviado ao novo e-mail")
    @PostMapping("/me/email-change/confirm")
    public ResponseEntity<Void> confirmarTrocaEmail(
            @Valid @RequestBody com.josenetoo_dev.veiculos_api.dto.usuario_dto.EmailChangeConfirmation request) {
        emailChangeService.confirmChange(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Dados do usuario autenticado")
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> meusDados() {
        return ResponseEntity
                .ok(usuarioService.buscarMeusDados());
    }

    @Operation
    @GetMapping
    public ResponseEntity<Page<UsuarioResponse>> verUsuarios(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity
                .ok(usuarioService.listarUsuarios(pageable));
    }

    @Operation(summary = "Buscar usuario por id")
    @GetMapping("/{id}")
    public ResponseEntity<com.josenetoo_dev.veiculos_api.dto.usuario_dto.UsuarioPublicResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity
                .ok(usuarioService.buscarPorId(id));
    }

    @Operation(summary = "Buscar usuarios por nome")
    @GetMapping("/buscar")
    public ResponseEntity<Page<UsuarioResponse>> buscarPorNome(
            @RequestParam String nome,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity
                .ok(usuarioService.buscarPorNome(nome, pageable));
    }

    @Operation(summary = "Atualizar usuario")
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizarUsuario(@Valid @RequestBody UsuarioRequest request, @PathVariable Long id) {
        return ResponseEntity
                .ok(usuarioService.atualizarUsuario(request, id));
    }

    @Operation(summary = "Trocar senha do usuario")
    @PutMapping("/{id}/senha")
    public ResponseEntity<UsuarioResponse> atualizarSenha(@Valid @RequestBody TrocarSenhaRequest request, @PathVariable Long id) {
        return ResponseEntity
                .ok(usuarioService.atualizarSenha(request, id));
    }

    @Operation(summary = "Deletar usuario")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable Long id) {
        usuarioService.deletarUsuario(id);

        return ResponseEntity.noContent().build();
    }

}
