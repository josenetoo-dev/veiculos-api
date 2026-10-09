package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.usuario_dto.EmailChangeConfirmation;
import com.josenetoo_dev.veiculos_api.dto.usuario_dto.EmailChangeRequest;
import com.josenetoo_dev.veiculos_api.exception.ex.CredenciaisInvalidasException;
import com.josenetoo_dev.veiculos_api.exception.ex.EmailChangeThrottledException;
import com.josenetoo_dev.veiculos_api.exception.ex.EmailChangeUnavailableException;
import com.josenetoo_dev.veiculos_api.exception.ex.EmailJaCadastradoException;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Confirma a posse do e-mail novo antes de trocar o login.
 * O segredo é criptograficamente aleatório, armazenado apenas como SHA-256,
 * temporário e de uso único. A senha atual é exigida para iniciar.
 */
@Service
@RequiredArgsConstructor
public class EmailChangeService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwords;
    private final EmailChangeMailer mailer;

    private Usuario lockedCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new CredenciaisInvalidasException("Usuário não autenticado");
        }
        try {
            Usuario usuario = usuarios.findByIdForUpdate(Long.valueOf(auth.getName()))
                    .orElseThrow(() -> new CredenciaisInvalidasException("Conta indisponível"));
            if (!usuario.getStatus().podeAutenticar()) {
                throw new CredenciaisInvalidasException("Conta indisponível");
            }
            return usuario;
        } catch (NumberFormatException e) {
            throw new CredenciaisInvalidasException("Conta indisponível");
        }
    }

    @Transactional
    public void requestChange(EmailChangeRequest request) {
        if (!mailer.isConfigured()) throw new EmailChangeUnavailableException();
        Usuario user = lockedCurrentUser();
        if (!passwords.matches(request.currentPassword(), user.getSenha())) {
            throw new CredenciaisInvalidasException("Credenciais inválidas");
        }

        String target = request.newEmail().trim().toLowerCase(Locale.ROOT);
        if (user.getEmail().equalsIgnoreCase(target)) {
            throw new IllegalArgumentException("Endereço de e-mail não foi alterado");
        }
        if (usuarios.existsByEmailAndIdNot(target, user.getId())) {
            throw new EmailJaCadastradoException("Endereço indisponível");
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (user.getPendingEmailRequestedAt() != null
                && user.getPendingEmailRequestedAt().isAfter(now.minusMinutes(1))) {
            throw new EmailChangeThrottledException();
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        user.setPendingEmail(target);
        user.setPendingEmailTokenHash(hash(code));
        user.setPendingEmailExpiresAt(now.plusMinutes(15));
        user.setPendingEmailRequestedAt(now);
        usuarios.saveAndFlush(user);
        // Se a entrega falhar, a transação é revertida; nenhum código é devolvido na resposta HTTP.
        mailer.sendConfirmation(target, code);
    }

    @Transactional
    public void confirmChange(EmailChangeConfirmation confirmation) {
        Usuario user = lockedCurrentUser();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (user.getPendingEmail() == null || user.getPendingEmailTokenHash() == null
                || user.getPendingEmailExpiresAt() == null
                || !user.getPendingEmailExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException("Código inválido ou expirado");
        }

        byte[] actual = hash(confirmation.token()).getBytes(StandardCharsets.US_ASCII);
        byte[] expected = user.getPendingEmailTokenHash().getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(actual, expected)) {
            throw new IllegalArgumentException("Código inválido ou expirado");
        }
        if (usuarios.existsByEmailAndIdNot(user.getPendingEmail(), user.getId())) {
            throw new EmailJaCadastradoException("Endereço indisponível");
        }

        user.setEmail(user.getPendingEmail());
        user.setPendingEmail(null);
        user.setPendingEmailTokenHash(null);
        user.setPendingEmailExpiresAt(null);
        user.setPendingEmailRequestedAt(null);
        // Um desafio emitido para o e-mail anterior não valida o endereço novo.
        user.setContactEmailTokenHash(null);
        user.setContactEmailExpiresAt(null);
        user.setContactEmailRequestedAt(null);
        user.setTokenVersion(user.getTokenVersion() + 1); // Revoga todos os tokens anteriores.
        usuarios.saveAndFlush(user);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo de hash indisponível", e);
        }
    }
}
