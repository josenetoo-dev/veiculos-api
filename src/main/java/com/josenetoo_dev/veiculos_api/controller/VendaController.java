package com.josenetoo_dev.veiculos_api.controller;

import com.josenetoo_dev.veiculos_api.dto.comercial.VendaDeclaradaResponse;
import com.josenetoo_dev.veiculos_api.service.VendaDeclaradaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/vendas")
@RequiredArgsConstructor
public class VendaController {
    private final VendaDeclaradaService service;
    @GetMapping("/minhas")
    public Page<VendaDeclaradaResponse> minhas(@PageableDefault(size = 20) Pageable pageable) {
        return service.minhas(pageable);
    }
}
