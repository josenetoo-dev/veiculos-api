package com.josenetoo_dev.veiculos_api;

import com.josenetoo_dev.veiculos_api.enums.*;
import com.josenetoo_dev.veiculos_api.repository.*;
import com.josenetoo_dev.veiculos_api.staging.StagingDemoData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/** Dados fictícios só sob profile staging e opt-in; teste NÃO usa rede/AWS. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:staging_demo;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "jwt.secret=staging-test-only-secret-at-least-32-bytes",
    "app.staging.demo.enabled=true",
    "STAGING_DEMO_ADMIN_PASSWORD=staging-only-admin-password-123",
    "STAGING_DEMO_BUYER_PASSWORD=staging-only-buyer-password-123",
    "verification.biometric.enabled=false"
})
@ActiveProfiles("staging")
class StagingDemoDataTest {
    @Autowired StagingDemoData seeder;
    @Autowired UsuarioRepository users;
    @Autowired AnuncioRepository ads;
    @Autowired PasswordEncoder passwords;

    @Test void demoCreatesOnlySyntheticUsersAndAdsAndIsIdempotent() {
        assertEquals(3, users.count());
        assertEquals(5, ads.count());
        var admin=users.findByEmail("admin-demo@auto-minas.test").orElseThrow();
        var buyer=users.findByEmail("comprador-demo@auto-minas.test").orElseThrow();
        assertEquals(Role.ADMIN, admin.getRole());
        assertEquals(Role.USER, buyer.getRole());
        assertEquals(StatusUsuario.ACTIVE, admin.getStatus());
        assertEquals(StatusUsuario.ACTIVE, buyer.getStatus());
        assertTrue(passwords.matches("staging-only-admin-password-123",admin.getSenha()));
        assertTrue(passwords.matches("staging-only-buyer-password-123",buyer.getSenha()));
        assertEquals(5, ads.findByStatus(StatusAnuncio.ATIVO,
                org.springframework.data.domain.Pageable.unpaged()).getTotalElements());
        ads.findAll().forEach(ad -> {
            assertTrue(ad.getTitulo().startsWith("[DEMO]"));
            assertTrue(ad.getDescricao().contains("FICTÍCIO"));
            assertNotNull(ad.getVeiculo());
            assertEquals("vendedor-demo@auto-minas.test", ad.getUsuario().getEmail());
        });

        seeder.run(new DefaultApplicationArguments(new String[0]));
        assertEquals(3, users.count());
        assertEquals(5, ads.count());
    }
}
