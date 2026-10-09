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
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Transactional(readOnly = true)
    private Usuario verificarId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuario não encontrado"));
    }

    private Usuario verificarIdParaAlteracao(Long id) {
        Usuario usuario = usuarioRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuario não encontrado"));
        // A entidade pode já estar no persistence context desde uma leitura anterior ao lock.
        // Refresh sob o mesmo lock evita regravar senha/status/tokenVersion de um snapshot antigo.
        entityManager.refresh(usuario, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (!usuario.getStatus().podeAutenticar()) {
            throw new CredenciaisInvalidasException("Conta indisponível");
        }
        return usuario;
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
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
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

        Usuario usuario = verificarIdParaAlteracao(id);

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

        Usuario usuario = verificarIdParaAlteracao(id);

        if (!passwordEncoder.matches(request.getSenhaAtual(), usuario.getSenha())) {
            throw new CredenciaisInvalidasException("Senha atual incorreta");
        }

        usuario.setSenha(passwordEncoder.encode(request.getNovaSenha()));
        usuario.setTokenVersion(usuario.getTokenVersion() + 1);

        return new UsuarioResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public void deletarUsuario(Long id) {
        Usuario usuarioAutenticado = obterUsuarioAutenticado();
        exigirProprioUsuario(usuarioAutenticado, id);

        Usuario usuario = verificarIdParaAlteracao(id);
        usuario.setStatus(com.josenetoo_dev.veiculos_api.enums.StatusUsuario.DELETED);
        usuario.setTokenVersion(usuario.getTokenVersion() + 1);
        usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public com.josenetoo_dev.veiculos_api.dto.usuario_dto.UsuarioPublicResponse buscarPorId(Long id) {
        return new com.josenetoo_dev.veiculos_api.dto.usuario_dto.UsuarioPublicResponse(verificarId(id));
    }

    @Transactional(readOnly = true)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public Page<UsuarioResponse> buscarPorNome(String nome, Pageable pageable) {
        return usuarioRepository.findByNomeContainingIgnoreCase(nome, pageable)
                .map(UsuarioResponse::new);
    }

}
