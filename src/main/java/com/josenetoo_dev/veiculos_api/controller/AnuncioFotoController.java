package com.josenetoo_dev.veiculos_api.controller;

import com.josenetoo_dev.veiculos_api.dto.anuncio_foto_dto.AnuncioFotoResponse;
import com.josenetoo_dev.veiculos_api.service.AnuncioFotoService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/anuncio/{anuncioId}/fotos")
@RequiredArgsConstructor
public class AnuncioFotoController {

    private final AnuncioFotoService anuncioFotoService;

    @Value("${upload.dir}")
    private String uploadDir;

    @Value("${app.public-base-url}")
    private String publicBaseUrl;

    @Operation(summary = "Fazer upload de fotos para um anúncio")
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<List<AnuncioFotoResponse>> uploadFotos(
            @PathVariable Long anuncioId,
            @RequestParam("fotos") List<MultipartFile> arquivos) throws IOException {

        Path pastaUpload = Paths.get(uploadDir).toAbsolutePath();
        if (!Files.exists(pastaUpload)) {
            Files.createDirectories(pastaUpload);
        }

        List<com.josenetoo_dev.veiculos_api.dto.anuncio_foto_dto.AnuncioFotoRequest> requests = new ArrayList<>();

        for (int i = 0; i < arquivos.size(); i++) {
            MultipartFile arquivo = arquivos.get(i);

            String extensao = obterExtensao(arquivo.getOriginalFilename());
            String nomeArquivo = UUID.randomUUID() + extensao;

            Path destino = pastaUpload.resolve(nomeArquivo);
            try (var inputStream = arquivo.getInputStream()) {
                Files.copy(inputStream, destino, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            String urlPublica = publicBaseUrl + "/uploads/fotos/" + nomeArquivo;

            com.josenetoo_dev.veiculos_api.dto.anuncio_foto_dto.AnuncioFotoRequest req =
                    new com.josenetoo_dev.veiculos_api.dto.anuncio_foto_dto.AnuncioFotoRequest();
            req.setUrl(urlPublica);
            req.setOrdem(i);
            req.setTipoFoto(com.josenetoo_dev.veiculos_api.enums.TipoFoto.FRENTE);

            requests.add(req);
        }

        List<AnuncioFotoResponse> fotos = anuncioFotoService.adicionarFotos(anuncioId, requests);

        return ResponseEntity.status(HttpStatus.CREATED).body(fotos);
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

    private String obterExtensao(String nomeOriginal) {
        if (nomeOriginal == null || !nomeOriginal.contains(".")) {
            return ".jpg";
        }
        return nomeOriginal.substring(nomeOriginal.lastIndexOf(".")).toLowerCase();
    }
}