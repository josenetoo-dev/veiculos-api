package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.auth.LoginRequest;
import com.josenetoo_dev.veiculos_api.dto.auth.LoginResponse;
import com.josenetoo_dev.veiculos_api.dto.auth.RegisterRequest;
import com.josenetoo_dev.veiculos_api.dto.usuario_dto.UsuarioResponse;
import com.josenetoo_dev.veiculos_api.exception.ex.CredenciaisInvalidasException;
import com.josenetoo_dev.veiculos_api.exception.ex.EmailJaCadastradoException;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import com.josenetoo_dev.veiculos_api.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public UsuarioResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new EmailJaCadastradoException("Email já cadastrado");
        }

        Usuario usuario = new Usuario();

        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setTelefone(request.getTelefone());
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));

        return new UsuarioResponse(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CredenciaisInvalidasException("Email ou senha inválidos"));

        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
            throw new CredenciaisInvalidasException("Email ou senha inválidos");
        }

        String token = jwtUtil.gerarToken(String.valueOf(usuario.getId()));

        return new LoginResponse(token);
    }

}
