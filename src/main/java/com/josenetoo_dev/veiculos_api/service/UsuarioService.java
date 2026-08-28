package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.usuario_dto.TrocarSenhaRequest;
import com.josenetoo_dev.veiculos_api.dto.usuario_dto.UsuarioRequest;
import com.josenetoo_dev.veiculos_api.dto.usuario_dto.UsuarioResponse;
import com.josenetoo_dev.veiculos_api.exception.ex.AcessoNegadoException;
import com.josenetoo_dev.veiculos_api.exception.ex.CredenciaisInvalidasException;
import com.josenetoo_dev.veiculos_api.exception.ex.EmailJaCadastradoException;
import com.josenetoo_dev.veiculos_api.exception.ex.UsuarioNaoEncontradoException;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    private Usuario verificarId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuario não encontrado"));
    }

    private Usuario obterUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new CredenciaisInvalidasException("Usuário não autenticado");
        }

        Long id;
        try {
            id = Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new CredenciaisInvalidasException("Usuário autenticado não encontrado");
        }

        return usuarioRepository.findById(id)
                .orElseThrow(() -> new CredenciaisInvalidasException("Usuário autenticado não encontrado"));
    }

    private void exigirProprioUsuario(Usuario usuarioAutenticado, Long id) {
        if (!usuarioAutenticado.getId().equals(id)) {
            throw new AcessoNegadoException("Você só pode alterar os seus próprios dados");
        }
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listarUsuarios(Pageable pageable) {
        return usuarioRepository.findAll(pageable)
                .map(UsuarioResponse::new);
    }

    // Dados do usuário autenticado (descoberto pelo token, sem varrer a listagem)
    @Transactional(readOnly = true)
    public UsuarioResponse buscarMeusDados() {
        return new UsuarioResponse(obterUsuarioAutenticado());
    }

    @Transactional
    public UsuarioResponse atualizarUsuario(UsuarioRequest request, Long id) {
        Usuario usuarioAutenticado = obterUsuarioAutenticado();
        exigirProprioUsuario(usuarioAutenticado, id);

        Usuario usuario = verificarId(id);

        if (usuarioRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new EmailJaCadastradoException("Email Já cadastrado exception");
        }

        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setTelefone(request.getTelefone());

        return new UsuarioResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse atualizarSenha(TrocarSenhaRequest request, Long id) {
        Usuario usuarioAutenticado = obterUsuarioAutenticado();
        exigirProprioUsuario(usuarioAutenticado, id);

        Usuario usuario = verificarId(id);

        if (!passwordEncoder.matches(request.getSenhaAtual(), usuario.getSenha())) {
            throw new CredenciaisInvalidasException("Senha atual incorreta");
        }

        usuario.setSenha(passwordEncoder.encode(request.getNovaSenha()));

        return new UsuarioResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public void deletarUsuario(Long id) {
        Usuario usuarioAutenticado = obterUsuarioAutenticado();
        exigirProprioUsuario(usuarioAutenticado, id);

        usuarioRepository.delete(verificarId(id));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        return new UsuarioResponse(verificarId(id));
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> buscarPorNome(String nome, Pageable pageable) {
        return usuarioRepository.findByNomeContainingIgnoreCase(nome, pageable)
                .map(UsuarioResponse::new);
    }

}
