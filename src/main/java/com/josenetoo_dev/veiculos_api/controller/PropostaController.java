package com.josenetoo_dev.veiculos_api.controller;

import com.josenetoo_dev.veiculos_api.dto.proposta_dto.PropostaRequest;
import com.josenetoo_dev.veiculos_api.dto.proposta_dto.PropostaResponse;
import com.josenetoo_dev.veiculos_api.service.PropostaService;
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
@RequestMapping("/v1/proposta")
@RequiredArgsConstructor
public class PropostaController {
    private final PropostaService propostaService;

    @Operation(summary = "Mandar proposta")
    @PostMapping
    public ResponseEntity<PropostaResponse> mandarProposta(@Valid @RequestBody PropostaRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(propostaService.mandarProposta(request));
    }

    @Operation
    @GetMapping
    public ResponseEntity<Page<PropostaResponse>> listarPropostas(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity
                .ok(propostaService.listarPropostas(pageable));
    }

    @Operation(summary = "Buscar proposta por id")
    @GetMapping("/{id}")
    public ResponseEntity<PropostaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity
                .ok(new PropostaResponse(propostaService.buscarPorId(id)));
    }

    @Operation(summary = "Cancelar proposta")
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<PropostaResponse> cancelarProposta(@PathVariable Long id) {
        return ResponseEntity
                .ok(propostaService.cancelarProposta(id));
    }

    @Operation(summary = "Aceitar proposta")
    @PutMapping("/{id}/aceitar")
    public ResponseEntity<PropostaResponse> aceitarProposta(@PathVariable Long id) {
        return ResponseEntity
                .ok(propostaService.aceitarProposta(id));
    }

    @Operation(summary = "Negar proposta")
    @PutMapping("/{id}/negar")
    public ResponseEntity<PropostaResponse> negarProposta(@PathVariable Long id) {
        return ResponseEntity
                .ok(propostaService.negarProposta(id));
    }

}
