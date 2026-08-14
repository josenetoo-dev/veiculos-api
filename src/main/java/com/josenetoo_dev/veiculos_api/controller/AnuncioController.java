package com.josenetoo_dev.veiculos_api.controller;

import com.josenetoo_dev.veiculos_api.dto.anuncio_dto.AnuncioRequest;
import com.josenetoo_dev.veiculos_api.dto.anuncio_dto.AnuncioResponse;
import com.josenetoo_dev.veiculos_api.enums.Categoria;
import com.josenetoo_dev.veiculos_api.enums.StatusAnuncio;
import com.josenetoo_dev.veiculos_api.service.AnuncioService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/anuncio")
@RequiredArgsConstructor
public class AnuncioController {
    private final AnuncioService anuncioService;

    @Operation(summary = "Criar anuncio")
    @PostMapping
    public ResponseEntity<AnuncioResponse> criarAnuncio(@Valid @RequestBody AnuncioRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(anuncioService.criarAnuncio(request));
    }

    @Operation
    @GetMapping
    public ResponseEntity<Page<AnuncioResponse>> verAnuncios(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity
                .ok(anuncioService.listarAnuncios(pageable));
    }

    @Operation(summary = "Buscar anuncio por id")
    @GetMapping("/{id}")
    public ResponseEntity<AnuncioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity
                .ok(anuncioService.buscarPorId(id));
    }

    @Operation(summary = "Buscar anuncio por codigo")
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<AnuncioResponse> buscarPorCodigo(@PathVariable String codigo) {
        return ResponseEntity
                .ok(anuncioService.buscarPorCodigo(codigo));
    }

    @Operation(summary = "Listar anuncios em destaque")
    @GetMapping("/destaques")
    public ResponseEntity<Page<AnuncioResponse>> listarDestaques(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity
                .ok(anuncioService.listarDestaques(pageable));
    }

    @Operation(summary = "Listar anuncios por status")
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<AnuncioResponse>> listarPorStatus(
            @PathVariable StatusAnuncio status,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity
                .ok(anuncioService.listarPorStatus(status, pageable));
    }

    @Operation(summary = "Listar anuncios por categoria")
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<Page<AnuncioResponse>> listarPorCategoria(
            @PathVariable Categoria categoria,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity
                .ok(anuncioService.listarPorCategoria(categoria, pageable));
    }

    @Operation(summary = "Atualizar anuncio")
    @PutMapping("/{id}")
    public ResponseEntity<AnuncioResponse> atualizarAnuncio(@Valid @RequestBody AnuncioRequest request, @PathVariable Long id) {
        return ResponseEntity
                .ok(anuncioService.atualizarAnuncio(request, id));
    }

    @Operation(summary = "Deletar anuncio")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarAnuncio(@PathVariable Long id) {
        anuncioService.deletarAnuncio(id);

        return ResponseEntity.noContent().build();
    }

}
