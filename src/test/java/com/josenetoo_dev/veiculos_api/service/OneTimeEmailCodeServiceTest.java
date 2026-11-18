package com.josenetoo_dev.veiculos_api.service;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OneTimeEmailCodeServiceTest {
    private static final String SECRET = "a-stable-test-secret-longer-than-32-bytes";

    @Test void generatesSixDigitsAndUsesKeyedHmac() {
        JdbcTemplate jdbc=mock(JdbcTemplate.class);
        OneTimeEmailCodeService service=new OneTimeEmailCodeService(jdbc,SECRET);
        String code=service.reserve();
        assertTrue(code.matches("[0-9]{6}"),"Código deve ter seis dígitos, incluindo zeros à esquerda");
        assertEquals(64,service.hash(code).length());
        assertNotEquals(code,service.hash(code));
        assertEquals(service.hash(code),new OneTimeEmailCodeService(mock(JdbcTemplate.class),SECRET).hash(code));
    }

    @Test void retriesWhenDatabaseDetectsAnActiveCollision() {
        JdbcTemplate jdbc=mock(JdbcTemplate.class);
        when(jdbc.update(startsWith("INSERT INTO email_code_reservation"),any(),any(),any()))
            .thenThrow(new DuplicateKeyException("duplicate active code"))
            .thenReturn(1);
        String code=new OneTimeEmailCodeService(jdbc,SECRET).reserve();
        assertTrue(code.matches("[0-9]{6}"));
        verify(jdbc,times(2)).update(startsWith("INSERT INTO email_code_reservation"),any(),any(),any());
    }

    @Test void preservesLegacyChallengeCompatibilityForExpiryWindow() {
        OneTimeEmailCodeService service=new OneTimeEmailCodeService(mock(JdbcTemplate.class),SECRET);
        String legacy="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQ";
        assertEquals(43,legacy.length());
        assertEquals(64,service.hash(legacy).length());
        assertNotEquals(service.hash(legacy),service.hash("012345"));
    }
}
