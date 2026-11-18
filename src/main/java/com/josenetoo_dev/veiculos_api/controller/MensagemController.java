package com.josenetoo_dev.veiculos_api.controller;

import com.josenetoo_dev.veiculos_api.dto.mensagem_dto.MensagemRequest;
import com.josenetoo_dev.veiculos_api.dto.mensagem_dto.MensagemResponse;
import com.josenetoo_dev.veiculos_api.service.MensagemService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/v1/proposta/{propostaId}/mensagens")
@RequiredArgsConstructor
public class MensagemController {
    private final MensagemService mensagemService;
    private final com.josenetoo_dev.veiculos_api.service.ChatEvents chatEvents;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter acompanhar(
            @PathVariable Long propostaId) {
        // Verificação no servidor, antes da assinatura. Nunca confiar apenas no BFF.
        mensagemService.verificarParticipante(propostaId);
        return chatEvents.subscribe(propostaId);
    }

    @Operation(summary = "Mandar mensagem no chat de uma proposta")
    @PostMapping
    public ResponseEntity<MensagemResponse> enviarMensagem(
            @PathVariable Long propostaId,
            @Valid @RequestBody MensagemRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mensagemService.enviarMensagem(propostaId, request));
    }

    @Operation(summary = "Listar mensagens do chat de uma proposta")
    @GetMapping
    public ResponseEntity<Page<MensagemResponse>> listarMensagens(
            @PathVariable Long propostaId,
            @PageableDefault(size = 50, sort = "criadoEm") Pageable pageable) {
        return ResponseEntity
                .ok(mensagemService.listarMensagens(propostaId, pageable));
    }

    @PostMapping("/leitura")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarLidas(@PathVariable Long propostaId, @Valid @RequestBody Leitura leitura) {
        mensagemService.marcarLidas(propostaId, leitura.ids());
    }

    public record Leitura(@jakarta.validation.constraints.NotEmpty
            @jakarta.validation.constraints.Size(max = 200) java.util.List<@jakarta.validation.constraints.Positive Long> ids) {}

    @Operation(summary = "Excluir logicamente mensagem própria enviada há até cinco minutos")
    @DeleteMapping("/{mensagemId}")
    public ResponseEntity<Void> excluirMensagem(@PathVariable Long propostaId, @PathVariable Long mensagemId) {
        mensagemService.excluirMensagem(propostaId, mensagemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/anexos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MensagemResponse> enviarImagem(@PathVariable Long propostaId,
            @RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mensagemService.enviarImagem(propostaId, arquivo));
    }

    @GetMapping("/{mensagemId}/arquivo")
    public ResponseEntity<byte[]> baixarImagem(@PathVariable Long propostaId, @PathVariable Long mensagemId) {
        var image = mensagemService.baixarImagem(propostaId, mensagemId);
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store, private")
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"imagem\"")
                .contentType(MediaType.parseMediaType(image.mime())).body(image.bytes());
    }
}
