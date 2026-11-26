package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.enums.StatusUsuario;
import com.josenetoo_dev.veiculos_api.exception.ex.CredenciaisInvalidasException;
import com.josenetoo_dev.veiculos_api.model.LoginSession;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.LoginSessionRepository;
import com.josenetoo_dev.veiculos_api.security.JwtUtil;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PersistentLoginServiceTest {
    @Test
    void expiresAfterSixtyDaysAndRejectsRevokedOrInvalidatedSessions() {
        LoginSessionRepository repository = mock(LoginSessionRepository.class);
        JwtUtil jwt = mock(JwtUtil.class);
        Usuario user = new Usuario();
        user.setId(42L);
        user.setStatus(StatusUsuario.ACTIVE);
        user.setTokenVersion(3);
        when(jwt.gerarToken("42")).thenReturn("short-lived-jwt");
        AtomicReference<LoginSession> saved = new AtomicReference<>();
        when(repository.save(any(LoginSession.class))).thenAnswer(invocation -> {
            LoginSession session = invocation.getArgument(0);
            saved.set(session);
            return session;
        });
        when(repository.findByTokenHash(any())).thenAnswer(invocation ->
                Optional.ofNullable(saved.get()).filter(session -> session.getTokenHash().equals(invocation.getArgument(0))));
        PersistentLoginService service = new PersistentLoginService(repository, jwt);

        var login = service.create(user);
        assertEquals("short-lived-jwt", login.getToken());
        assertEquals(60, java.time.Duration.between(saved.get().getCreatedAt(), saved.get().getExpiresAt()).toDays());
        assertNotEquals(login.getRefreshToken(), saved.get().getTokenHash());
        assertEquals("short-lived-jwt", service.refresh(login.getRefreshToken()).getToken());

        saved.get().setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        assertThrows(CredenciaisInvalidasException.class, () -> service.refresh(login.getRefreshToken()));
        saved.get().setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(1));
        user.setTokenVersion(4);
        assertThrows(CredenciaisInvalidasException.class, () -> service.refresh(login.getRefreshToken()));
        user.setTokenVersion(3);
        service.revoke(login.getRefreshToken());
        assertThrows(CredenciaisInvalidasException.class, () -> service.refresh(login.getRefreshToken()));
    }
}
