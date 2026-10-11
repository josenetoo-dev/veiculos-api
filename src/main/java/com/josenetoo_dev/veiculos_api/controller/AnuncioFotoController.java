package com.josenetoo_dev.veiculos_api.controller;

import com.josenetoo_dev.veiculos_api.dto.anuncio_foto_dto.AnuncioFotoResponse;
import com.josenetoo_dev.veiculos_api.service.AnuncioFotoService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/v1/anuncio/{anuncioId}/fotos")
@RequiredArgsConstructor
public class AnuncioFotoController {

    private final AnuncioFotoService anuncioFotoService;

    @Operation(summary = "Fazer upload de fotos para um anúncio")
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<List<AnuncioFotoResponse>> uploadFotos(
            @PathVariable Long anuncioId,
            @RequestParam("fotos") List<MultipartFile> arquivos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(anuncioFotoService.uploadFotos(anuncioId, arquivos));
    }

    @Operation(summary = "Listar fotos de um anúncio")
    @GetMapping
    public ResponseEntity<Page<AnuncioFotoResponse>> listarFotos(
            @PathVariable Long anuncioId,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(anuncioFotoService.listarFotosPorAnuncio(anuncioId, pageable));
    }

    @Operation(summary = "Deletar uma foto")
    @DeleteMapping("/{fotoId}")
    public ResponseEntity<Void> deletarFoto(
            @PathVariable Long anuncioId,
            @PathVariable Long fotoId) {

        anuncioFotoService.deletarFoto(anuncioId, fotoId);
        return ResponseEntity.noContent().build();
    }

}
