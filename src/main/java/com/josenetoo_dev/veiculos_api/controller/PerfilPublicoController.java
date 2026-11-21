package com.josenetoo_dev.veiculos_api.controller;

import com.josenetoo_dev.veiculos_api.enums.*;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.*;
import com.josenetoo_dev.veiculos_api.service.ImageValidator;
import com.josenetoo_dev.veiculos_api.service.PrivateEvidenceStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.josenetoo_dev.veiculos_api.exception.ex.UsuarioNaoEncontradoException;
import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class PerfilPublicoController {
    private final UsuarioRepository usuarios;
    private final VerificacaoRepository verificacoes;
    private final AnuncioRepository anuncios;
    private final PrivateEvidenceStorage storage;
    private final ImageValidator imagens;

    public record Veiculo(long id, String titulo) {}
    public record Perfil(String nome, boolean temFoto, int nivel, List<Veiculo> veiculos, boolean temMais, boolean equipeAutoMinas) {}

    private Usuario visivel(Long id) {
        return usuarios.findById(id).filter(u -> u.getStatus().podeAutenticar())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Perfil não encontrado"));
    }

    @GetMapping("/v1/perfis/{id}")
    @Transactional(readOnly = true)
    public Perfil perfil(@PathVariable Long id, @RequestParam(defaultValue = "0") int pagina) {
        var usuario = visivel(id);
        var ativos = anuncios.findByStatusAndUsuarioId(StatusAnuncio.ATIVO, id,
                PageRequest.of(Math.max(0, pagina), 24, org.springframework.data.domain.Sort.by("id").descending()));
        boolean temFoto = usuario.getFotoPerfilChave() != null;
        boolean nivelUm = temFoto && usuario.getStatus() == StatusUsuario.ACTIVE
                && verificacoes.existsByUsuarioIdentidadeIdAndStatus(id, StatusVerificacao.APROVADA)
                && anuncios.contarVeiculosRevisados(id, StatusVerificacao.APROVADA) > 0;
        boolean equipeAutoMinas = usuario.getRole() == Role.ADMIN || usuario.getRole() == Role.REVIEWER;
        return new Perfil(usuario.getNome(), temFoto, nivelUm ? 1 : 0,
                ativos.map(a -> new Veiculo(a.getId(), a.getTitulo())).getContent(), ativos.hasNext(), equipeAutoMinas);
    }

    @GetMapping("/v1/perfis/{id}/foto")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> foto(@PathVariable Long id) {
        var usuario = visivel(id);
        if (usuario.getFotoPerfilChave() == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff")
                .contentType(MediaType.parseMediaType(usuario.getFotoPerfilTipo())).body(storage.read(usuario.getFotoPerfilChave()));
    }

    @PostMapping(value = "/v1/usuario/me/foto-perfil", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void atualizar(Principal principal, @RequestParam MultipartFile arquivo) {
        var imagem = imagens.validate(arquivo);
        var dono = usuarios.findByIdForUpdate(Long.valueOf(principal.getName())).orElseThrow();
        var anterior = dono.getFotoPerfilChave();
        dono.setFotoPerfilChave(storage.store(imagem));
        dono.setFotoPerfilTipo(imagem.extension().equals("png") ? "image/png" : "image/jpeg");
        if (anterior != null) storage.removeAfterCommit(anterior);
    }
}
