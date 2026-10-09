package com.josenetoo_dev.veiculos_api.controller;

import com.josenetoo_dev.veiculos_api.dto.denuncia.*;
import com.josenetoo_dev.veiculos_api.service.DenunciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/denuncias")
@RequiredArgsConstructor
public class DenunciaController {
    private final DenunciaService service;

    @PostMapping
    public ResponseEntity<DenunciaResponse> registrar(@Valid @RequestBody DenunciaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(request));
    }

    @GetMapping("/minhas")
    public Page<DenunciaResponse> minhas(@PageableDefault(size=10) Pageable pageable) {
        return service.minhas(pageable);
    }

    @GetMapping("/{id}")
    public DenunciaResponse consultar(@PathVariable Long id) {
        return service.consultar(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    @GetMapping("/revisao/pendentes")
    public Page<DenunciaResponse> pendentes(@PageableDefault(size=10) Pageable pageable) {
        return service.pendentes(pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    @PostMapping("/revisao/{id}/confirmar")
    public DenunciaResponse confirmar(@PathVariable Long id,
                                     @Valid @RequestBody DecisaoDenunciaRequest request) {
        return service.decidir(id, true, request.motivo());
    }

    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    @PostMapping("/revisao/{id}/descartar")
    public DenunciaResponse descartar(@PathVariable Long id,
                                     @Valid @RequestBody DecisaoDenunciaRequest request) {
        return service.decidir(id, false, request.motivo());
    }
}
