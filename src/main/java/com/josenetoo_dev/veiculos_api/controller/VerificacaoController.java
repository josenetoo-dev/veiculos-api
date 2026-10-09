package com.josenetoo_dev.veiculos_api.controller;

import com.josenetoo_dev.veiculos_api.dto.verificacao.*;
import com.josenetoo_dev.veiculos_api.enums.TipoEvidencia;
import com.josenetoo_dev.veiculos_api.service.VerificacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * Envio protegido e revisão humana. Nunca retorna URLs públicas dos documentos.
 */
@RestController
@RequestMapping("/v1/verificacoes")
@RequiredArgsConstructor
public class VerificacaoController {
    private final VerificacaoService service;

    @PostMapping("/identidade")
    public ResponseEntity<VerificacaoResponse> iniciarIdentidade() {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.iniciarIdentidade());
    }

    @PostMapping("/veiculos/{veiculoId}")
    public ResponseEntity<VerificacaoResponse> iniciarVeiculo(@PathVariable Long veiculoId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.iniciarVeiculo(veiculoId));
    }

    @GetMapping("/minhas")
    public Page<VerificacaoResponse> minhas(@PageableDefault(size = 10) Pageable pageable) {
        return service.minhas(pageable);
    }

    @GetMapping("/{id}")
    public VerificacaoResponse consultar(@PathVariable Long id) {
        return service.consultar(id);
    }

    @PostMapping("/{id}/enviar")
    public VerificacaoResponse enviar(@PathVariable Long id) {
        return service.enviar(id);
    }

    @GetMapping("/{id}/evidencias")
    public List<EvidenciaResponse> evidencias(@PathVariable Long id) {
        return service.listarEvidencias(id);
    }

    @PostMapping(value = "/{id}/evidencias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EvidenciaResponse> upload(
            @PathVariable Long id, @RequestParam("tipo") TipoEvidencia tipo,
            @RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.adicionarEvidencia(id, tipo, arquivo));
    }

    @DeleteMapping("/{id}/evidencias/{evidenciaId}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, @PathVariable Long evidenciaId) {
        service.excluirEvidencia(id, evidenciaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/evidencias/{evidenciaId}/arquivo")
    public ResponseEntity<byte[]> baixar(@PathVariable Long id, @PathVariable Long evidenciaId) {
        var image = service.baixarEvidencia(id, evidenciaId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store, private")
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"evidencia\"")
                .contentType(MediaType.parseMediaType(image.mime()))
                .body(image.bytes());
    }

    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    @GetMapping("/revisao/pendentes")
    public Page<VerificacaoResponse> pendentes(@PageableDefault(size = 10) Pageable pageable) {
        return service.pendentes(pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    @PostMapping("/revisao/{id}/aprovar")
    public VerificacaoResponse aprovar(@PathVariable Long id) {
        return service.decidir(id, true, null);
    }

    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    @PostMapping("/revisao/{id}/rejeitar")
    public VerificacaoResponse rejeitar(@PathVariable Long id,
            @Valid @RequestBody RejeitarVerificacaoRequest request) {
        return service.decidir(id, false, request.motivo());
    }
}
