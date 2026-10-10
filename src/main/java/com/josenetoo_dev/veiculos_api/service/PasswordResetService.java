package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.auth.*;
import com.josenetoo_dev.veiculos_api.exception.ex.EmailChangeUnavailableException;
import com.josenetoo_dev.veiculos_api.exception.ex.InvalidPasswordResetTokenException;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

/**
 * Recuperação independente de sessão: posse da caixa postal é provada com
 * segredo forte e uso único. Não revela se o e-mail existe no sistema.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private static final SecureRandom RANDOM=new SecureRandom();
    private static final int MAX_FAILED_ATTEMPTS=5;
    private final UsuarioRepository users;
    private final PasswordEncoder passwords;
    private final PasswordResetMailer mailer;

    @Transactional
    public void request(PasswordResetStartRequest request) {
        // Disponibilidade é igual para e-mails cadastrados e não cadastrados.
        if (!mailer.configured()) throw new EmailChangeUnavailableException();

        Optional<Usuario> found=users.findByEmail(request.email().trim().toLowerCase(Locale.ROOT));
        if (found.isEmpty() || !found.get().getStatus().podeAutenticar()) return;
        Usuario user=users.findByIdForUpdate(found.get().getId()).orElseThrow();

        LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
        if (user.getPasswordResetRequestedAt()!=null &&
                user.getPasswordResetRequestedAt().isAfter(now.minusMinutes(1))) return;

        byte[] random=new byte[32];
        RANDOM.nextBytes(random);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        user.setPasswordResetTokenHash(hash(token));
        user.setPasswordResetExpiresAt(now.plusMinutes(15));
        user.setPasswordResetRequestedAt(now);
        user.setPasswordResetFailedAttempts(0);
        users.saveAndFlush(user);
        // Se SMTP falhar, rollback mantém o código antigo sem expor o novo.
        mailer.send(user.getEmail(),token);
    }

    @Transactional(noRollbackFor = InvalidPasswordResetTokenException.class)
    public void confirm(PasswordResetConfirmRequest request) {
        // Mesma mensagem de erro para endereço, token, expiração e status.
        String invalid="Código inválido ou expirado";
        Usuario user=users.findByEmail(request.email().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(()->new InvalidPasswordResetTokenException());
        user=users.findByIdForUpdate(user.getId()).orElseThrow();

        LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
        if (!user.getStatus().podeAutenticar()
                || user.getPasswordResetTokenHash()==null
                || user.getPasswordResetExpiresAt()==null
                || !user.getPasswordResetExpiresAt().isAfter(now)
                || user.getPasswordResetFailedAttempts()>=MAX_FAILED_ATTEMPTS) {
            throw new InvalidPasswordResetTokenException();
        }

        byte[] expected=user.getPasswordResetTokenHash().getBytes(StandardCharsets.US_ASCII);
        byte[] actual=hash(request.token()).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected,actual)) {
            int failures=user.getPasswordResetFailedAttempts()+1;
            user.setPasswordResetFailedAttempts(failures);
            if (failures>=MAX_FAILED_ATTEMPTS) {
                user.setPasswordResetTokenHash(null);
                user.setPasswordResetExpiresAt(null);
            }
            users.saveAndFlush(user);
            // No rollback para InvalidPasswordResetTokenException preserva as tentativas inválidas.
            throw new InvalidPasswordResetTokenException();
        }

        user.setSenha(passwords.encode(request.newPassword()));
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);
        user.setPasswordResetRequestedAt(null);
        user.setPasswordResetFailedAttempts(0);
        user.setTokenVersion(user.getTokenVersion()+1);
        users.saveAndFlush(user);
    }

    private static String hash(String value) {
        try {
            byte[] sum=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(sum);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível",ex);
        }
    }
}
