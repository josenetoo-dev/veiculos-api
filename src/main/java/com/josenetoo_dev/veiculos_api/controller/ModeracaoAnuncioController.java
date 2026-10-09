package com.josenetoo_dev.veiculos_api.controller;

import com.josenetoo_dev.veiculos_api.dto.anuncio_dto.AnuncioResponse;
import com.josenetoo_dev.veiculos_api.dto.anuncio_dto.RejeitarAnuncioRequest;
import com.josenetoo_dev.veiculos_api.service.ModeracaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/moderacao/anuncios")
@PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
@RequiredArgsConstructor
public class ModeracaoAnuncioController {
    private final ModeracaoService moderacao;

    @GetMapping
    public ResponseEntity<Page<AnuncioResponse>> pendentes(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(moderacao.pendentes(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnuncioResponse> consultar(@PathVariable Long id) {
        return ResponseEntity.ok(moderacao.consultar(id));
    }

    @PostMapping("/{id}/aprovar")
    public ResponseEntity<AnuncioResponse> aprovar(@PathVariable Long id) {
        return ResponseEntity.ok(moderacao.aprovar(id));
    }

    @PostMapping("/{id}/rejeitar")
    public ResponseEntity<AnuncioResponse> rejeitar(
            @PathVariable Long id, @Valid @RequestBody RejeitarAnuncioRequest request) {
        return ResponseEntity.ok(moderacao.rejeitar(id, request.motivo()));
    }
}
