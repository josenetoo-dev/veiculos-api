package com.josenetoo_dev.veiculos_api.controller;

import com.josenetoo_dev.veiculos_api.dto.comercial.*;
import com.josenetoo_dev.veiculos_api.service.PlanoComercialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/assinaturas")
@RequiredArgsConstructor
public class AssinaturaController {
    private final PlanoComercialService service;
    @PostMapping("/intencoes")
    public ResponseEntity<AssinaturaResponse> intencao(@Valid @RequestBody IntencaoAssinaturaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarIntencao(request));
    }
    @GetMapping("/minhas")
    public Page<AssinaturaResponse> minhas(@PageableDefault(size = 20) Pageable pageable) {
        return service.minhas(pageable);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public Page<AssinaturaResponse> todas(@PageableDefault(size = 50, sort = "criadoEm", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return service.todas(pageable);
    }
    @PostMapping("/{id}/cancelar")
    public AssinaturaResponse cancelar(@PathVariable Long id) { return service.cancelar(id); }
    @GetMapping("/{id}/historico")
    public java.util.List<EventoAssinaturaResponse> historico(@PathVariable Long id) { return service.historico(id); }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/confirmar-pagamento-externo")
    public AssinaturaResponse confirmar(@PathVariable Long id, @Valid @RequestBody ConfirmarPagamentoRequest request) {
        return service.confirmarPagamentoExterno(id, request);
    }
}
