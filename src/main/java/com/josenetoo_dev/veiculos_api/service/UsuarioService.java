package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.usuario_dto.UsuarioRequest;
import com.josenetoo_dev.veiculos_api.dto.usuario_dto.UsuarioResponse;
import com.josenetoo_dev.veiculos_api.exception.EmailJaCadastradoException;
import com.josenetoo_dev.veiculos_api.exception.UsuarioNaoEncontradoException;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;

    // CRUD basico + Buscar por nome e buscar por ID

    // Verificar por id
    @Transactional(readOnly = true)
    private Usuario verificarId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuario não encontrado"));
    }

    // Create
    @Transactional
    public UsuarioResponse criarUsuario(UsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new EmailJaCadastradoException("Email Já cadastrado exception");
        }

        Usuario usuario = new Usuario();

        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setTelefone(request.getTelefone());
        usuario.setSenha(request.getSenha());

        return new UsuarioResponse(usuarioRepository.save(usuario));
    }

    // Read com paginação
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listarUsuarios(Pageable pageable) {
        return usuarioRepository.findAll(pageable)
                .map(UsuarioResponse::new);
    }

    // Uppdate
    @Transactional
    public UsuarioResponse atualizarUsuario(UsuarioRequest request, Long id) {
        Usuario usuario = verificarId(id);

        if (usuarioRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new EmailJaCadastradoException("Email Já cadastrado exception");
        }

        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setTelefone(request.getTelefone());

        return new UsuarioResponse(usuarioRepository.save(usuario));
    }

    // Update da senha
    // TODO: implementar regras de segurança na troca de senha (validar senha atual)
    @Transactional
    public UsuarioResponse atualizarSenha(UsuarioRequest request, Long id) {
        Usuario usuario = verificarId(id);

        usuario.setSenha(request.getSenha());

        return new UsuarioResponse(usuarioRepository.save(usuario));
    }

    // Delete
    @Transactional
    public void deletarUsuario(Long id) {
        Usuario usuario = verificarId(id);
        // TODO: verificar se senha bate com a do banco (implementar com BCrypt depois)
        usuarioRepository.delete(usuario);
    }

    // buscar por id e nome
    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        return new UsuarioResponse(verificarId(id));
    }

    // buscar por nome
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> buscarPorNome(String nome, Pageable pageable) {
        return usuarioRepository.findByNomeContainingIgnoreCase(nome, pageable)
                .map(UsuarioResponse::new);
    }

}
