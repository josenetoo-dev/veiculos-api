package com.josenetoo_dev.veiculos_api.controller;

import com.josenetoo_dev.veiculos_api.dto.verificacao.*;
import com.josenetoo_dev.veiculos_api.service.BiometriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Somente dono com JWT: nenhum score, selfie, URL privada ou chave AWS retorna.
 * O frontend deve usar a sessão no FaceLivenessDetector e, no callback,
 * pedir ao backend para buscar o resultado diretamente na AWS.
 */
@RestController
@RequestMapping("/v1/verificacoes/{verificacaoId}/biometria")
@RequiredArgsConstructor
public class BiometriaController {
    private final BiometriaService service;

    @PostMapping("/sessoes")
    public ResponseEntity<SessaoBiometriaResponse> iniciar(
            @PathVariable Long verificacaoId,
            @Valid @RequestBody IniciarBiometriaRequest request) {
        // @AssertTrue registra que usuário declarou ciência da coleta biométrica;
        // não substitui a definição de base legal adequada sob LGPD.
        return ResponseEntity.status(HttpStatus.CREATED).body(service.iniciar(verificacaoId));
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/dispensar")
    public ResponseEntity<Void> dispensar(@PathVariable Long verificacaoId,
            @Valid @RequestBody DispensarBiometriaRequest request) {
        service.dispensarParaAnaliseHumana(verificacaoId,request.motivo());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessoes/{sessionId}/resultado")
    public SessaoBiometriaResponse resultado(
            @PathVariable Long verificacaoId, @PathVariable String sessionId) {
        return service.consultarResultado(verificacaoId,sessionId);
    }
}
