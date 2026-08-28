package com.josenetoo_dev.veiculos_api.controller;

import com.josenetoo_dev.veiculos_api.dto.mensagem_dto.MensagemRequest;
import com.josenetoo_dev.veiculos_api.dto.mensagem_dto.MensagemResponse;
import com.josenetoo_dev.veiculos_api.service.MensagemService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/proposta/{propostaId}/mensagens")
@RequiredArgsConstructor
public class MensagemController {
    private final MensagemService mensagemService;

    @Operation(summary = "Mandar mensagem no chat de uma proposta")
    @PostMapping
    public ResponseEntity<MensagemResponse> enviarMensagem(
            @PathVariable Long propostaId,
            @Valid @RequestBody MensagemRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mensagemService.enviarMensagem(propostaId, request));
    }

    @Operation(summary = "Listar mensagens do chat de uma proposta")
    @GetMapping
    public ResponseEntity<Page<MensagemResponse>> listarMensagens(
            @PathVariable Long propostaId,
            @PageableDefault(size = 50, sort = "criadoEm") Pageable pageable) {
        return ResponseEntity
                .ok(mensagemService.listarMensagens(propostaId, pageable));
    }
}
