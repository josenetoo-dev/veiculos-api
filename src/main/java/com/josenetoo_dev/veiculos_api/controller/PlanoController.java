package com.josenetoo_dev.veiculos_api.controller;

import com.josenetoo_dev.veiculos_api.dto.comercial.PlanoResponse;
import com.josenetoo_dev.veiculos_api.service.PlanoComercialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/v1/planos")
@RequiredArgsConstructor
public class PlanoController {
    private final PlanoComercialService service;
    @GetMapping public List<PlanoResponse> listar() { return service.listarPlanos(); }
    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.web.bind.annotation.PutMapping("/{codigo}/beneficios")
    public PlanoResponse atualizarBeneficios(@org.springframework.web.bind.annotation.PathVariable String codigo,
            @Valid @org.springframework.web.bind.annotation.RequestBody com.josenetoo_dev.veiculos_api.dto.comercial.AtualizarBeneficiosRequest request) {
        return service.atualizarBeneficios(codigo, request);
    }
}
