package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.auth.LoginResponse;
import com.josenetoo_dev.veiculos_api.exception.ex.CredenciaisInvalidasException;
import com.josenetoo_dev.veiculos_api.model.LoginSession;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.LoginSessionRepository;
import com.josenetoo_dev.veiculos_api.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class PersistentLoginService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final LoginSessionRepository sessions;
    private final JwtUtil jwt;

    @Transactional
    public LoginResponse create(Usuario user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LoginSession session = new LoginSession();
        session.setUsuario(user);
        session.setTokenHash(hash(refresh));
        session.setTokenVersion(user.getTokenVersion());
        session.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        session.setExpiresAt(session.getCreatedAt().plusDays(60));
        sessions.save(session);
        return new LoginResponse(jwt.gerarToken(user.getId().toString()), refresh);
    }

    @Transactional(readOnly = true)
    public LoginResponse refresh(String refresh) {
        LoginSession session = findValid(refresh);
        return new LoginResponse(jwt.gerarToken(session.getUsuario().getId().toString()), refresh);
    }

    @Transactional
    public void revoke(String refresh) {
        if (refresh == null || refresh.isBlank()) return;
        sessions.findByTokenHash(hash(refresh)).ifPresent(session -> {
            session.setRevokedAt(LocalDateTime.now(ZoneOffset.UTC));
            sessions.save(session);
        });
    }

    @Transactional
    public LoginResponse renew(String refresh, Long authenticatedId) {
        if (refresh == null || !refresh.matches("[A-Za-z0-9_-]{43}"))
            throw new CredenciaisInvalidasException("Sessão expirada");
        LoginSession previous = sessions.findByTokenHash(hash(refresh))
                .orElseThrow(() -> new CredenciaisInvalidasException("Sessão expirada"));
        if (previous.getRevokedAt() != null || !previous.getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))
                || !previous.getUsuario().getId().equals(authenticatedId))
            throw new CredenciaisInvalidasException("Sessão expirada");
        previous.setRevokedAt(LocalDateTime.now(ZoneOffset.UTC));
        sessions.save(previous);
        return create(previous.getUsuario());
    }

    private LoginSession findValid(String refresh) {
        if (refresh == null || !refresh.matches("[A-Za-z0-9_-]{43}"))
            throw new CredenciaisInvalidasException("Sessão expirada");
        LoginSession session = sessions.findByTokenHash(hash(refresh))
                .orElseThrow(() -> new CredenciaisInvalidasException("Sessão expirada"));
        Usuario user = session.getUsuario();
        if (session.getRevokedAt() != null || !session.getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))
                || session.getTokenVersion() != user.getTokenVersion() || !user.getStatus().podeAutenticar())
            throw new CredenciaisInvalidasException("Sessão expirada");
        return session;
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
