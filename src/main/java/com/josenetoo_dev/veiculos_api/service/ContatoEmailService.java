package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.usuario_dto.ConfirmarContatoEmailRequest;
import com.josenetoo_dev.veiculos_api.enums.StatusUsuario;
import com.josenetoo_dev.veiculos_api.exception.ex.*;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

/**
 * Confirma acesso ao e-mail cadastrado; não equivale à verificação de identidade.
 * Usa token forte de uso único, hash no banco, expiração e cooldown por usuário.
 */
@Service
@RequiredArgsConstructor
public class ContatoEmailService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UsuarioRepository usuarios;
    private final ContatoEmailMailer mailer;

    private Usuario userLocked() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new CredenciaisInvalidasException("Não autenticado");
        }
        try {
            return usuarios.findByIdForUpdate(Long.valueOf(auth.getName()))
                    .orElseThrow(() -> new CredenciaisInvalidasException("Conta inexistente"));
        } catch (NumberFormatException e) {
            throw new CredenciaisInvalidasException("Credenciais inválidas");
        }
    }

    @Transactional
    public void solicitar() {
        // Sem SMTP autorizado e configurado, o fluxo fica indisponível.
        if (!mailer.available()) throw new EmailChangeUnavailableException();
        Usuario usuario = userLocked();
        if (usuario.getStatus() != StatusUsuario.PENDING_CONTACT_VERIFICATION) {
            throw new VerificacaoIndisponivelException("Confirmação de contato não disponível neste estado");
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (usuario.getContactEmailRequestedAt() != null
                && usuario.getContactEmailRequestedAt().isAfter(now.minusMinutes(1))) {
            throw new EmailChangeThrottledException();
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        usuario.setContactEmailTokenHash(hash(token));
        usuario.setContactEmailExpiresAt(now.plusMinutes(15));
        usuario.setContactEmailRequestedAt(now);
        usuarios.saveAndFlush(usuario);
        // Falha SMTP reverte o desafio; nenhum código é revelado na API.
        mailer.send(usuario.getEmail(), token);
    }

    @Transactional
    public void confirmar(ConfirmarContatoEmailRequest request) {
        Usuario usuario = userLocked();
        if (usuario.getStatus() != StatusUsuario.PENDING_CONTACT_VERIFICATION
                || usuario.getContactEmailTokenHash() == null || usuario.getContactEmailExpiresAt() == null
                || !usuario.getContactEmailExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new IllegalArgumentException("Código inválido ou expirado");
        }
        byte[] expected = usuario.getContactEmailTokenHash().getBytes(StandardCharsets.US_ASCII);
        byte[] actual = hash(request.token()).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new IllegalArgumentException("Código inválido ou expirado");
        }
        usuario.setStatus(StatusUsuario.ACTIVE);
        usuario.setContactEmailVerifiedAt(LocalDateTime.now(ZoneOffset.UTC));
        usuario.setContactEmailTokenHash(null);
        usuario.setContactEmailExpiresAt(null);
        usuario.setContactEmailRequestedAt(null);
        usuario.setTokenVersion(usuario.getTokenVersion() + 1); // revoga JWT anterior
        usuarios.saveAndFlush(usuario);
    }

    private static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
