package com.josenetoo_dev.veiculos_api.controller;

import com.josenetoo_dev.veiculos_api.dto.comercial.*;
import com.josenetoo_dev.veiculos_api.service.ContratoComercialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/v1/contratos")
@RequiredArgsConstructor
public class ContratoController {
    private final ContratoComercialService service;
    @GetMapping("/atual") public ContratoResponse atual() { return service.atual(); }
    @GetMapping("/meus-aceites") public List<AceiteContratoResponse> meusAceites() { return service.meusAceites(); }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/versoes")
    public org.springframework.data.domain.Page<ContratoResponse> versoes(@org.springframework.data.web.PageableDefault(size = 50) org.springframework.data.domain.Pageable pageable) { return service.versoesAdmin(pageable); }
    @PostMapping("/{id}/aceites") public AceiteContratoResponse aceitar(@PathVariable Long id) { return service.aceitar(id); }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/versoes") public ContratoResponse criar(@Valid @RequestBody CriarContratoRequest request) {
        return service.criar(request);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/versoes/{id}/publicar") public ContratoResponse publicar(@PathVariable Long id) {
        return service.publicar(id);
    }
}
