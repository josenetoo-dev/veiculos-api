package com.josenetoo_dev.veiculos_api.controller;

import com.josenetoo_dev.veiculos_api.dto.verificacao.ResumoVerificacoesResponse;
import com.josenetoo_dev.veiculos_api.service.ResumoVerificacoesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/anuncio/{anuncioId}/verificacoes")
public class ResumoVerificacoesController {
    private final ResumoVerificacoesService service;

    @GetMapping
    public ResumoVerificacoesResponse consultar(@PathVariable Long anuncioId) {
        return service.consultar(anuncioId);
    }
}
