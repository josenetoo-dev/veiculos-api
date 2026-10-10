package com.josenetoo_dev.veiculos_api.controller;

import com.josenetoo_dev.veiculos_api.service.FotoPublicaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * Substitui o ResourceHandler desprotegido sem alterar o formato das URLs.
 * Cache sempre no-store para impedir acesso após desativação/revisão.
 */
@RestController
@RequestMapping("/uploads/fotos")
@RequiredArgsConstructor
public class FotoPublicaController {
    private final FotoPublicaService photos;

    @GetMapping("/{filename:.+}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable String filename) {
        var content=photos.read(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL,"no-store, max-age=0")
                .header("X-Content-Type-Options","nosniff")
                .contentType(MediaType.parseMediaType(content.mediaType()))
                .body(content.bytes());
    }
}
