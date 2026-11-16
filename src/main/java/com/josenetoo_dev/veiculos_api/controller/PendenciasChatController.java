package com.josenetoo_dev.veiculos_api.controller;

import com.josenetoo_dev.veiculos_api.service.MensagemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PendenciasChatController {
    private final MensagemService mensagens;

    @GetMapping("/v1/proposta/mensagens-nao-lidas")
    public Map<Long, Long> pendencias() {
        return mensagens.pendencias();
    }
}
