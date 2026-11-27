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
    "STAGING_DEMO_SELLER_PASSWORD=staging-only-seller-password-123",
    "verification.biometric.enabled=false"
})
@ActiveProfiles("staging")
class StagingDemoDataTest {
    @Autowired StagingDemoData seeder;
    @Autowired UsuarioRepository users;
    @Autowired AnuncioRepository ads;
    @Autowired AnuncioFotoRepository photos;
    @Autowired PropostaRepository proposals;
    @Autowired PasswordEncoder passwords;

    @Test void demoCreatesOnlySyntheticUsersAndAdsAndIsIdempotent() {
        assertEquals(3, users.count());
        assertEquals(160, ads.count());
        var admin=users.findByEmail("admin-demo@auto-minas.test").orElseThrow();
        var buyer=users.findByEmail("comprador-demo@auto-minas.test").orElseThrow();
        var seller=users.findByEmail("vendedor-demo@auto-minas.test").orElseThrow();
        assertEquals(Role.ADMIN, admin.getRole());
        assertEquals(Role.USER, buyer.getRole());
        assertEquals(StatusUsuario.ACTIVE, admin.getStatus());
        assertEquals(StatusUsuario.ACTIVE, buyer.getStatus());
        assertTrue(passwords.matches("staging-only-admin-password-123",admin.getSenha()));
        assertTrue(passwords.matches("staging-only-buyer-password-123",buyer.getSenha()));
        assertTrue(passwords.matches("staging-only-seller-password-123",seller.getSenha()));
        assertEquals(160, ads.findByStatus(StatusAnuncio.ATIVO,
                org.springframework.data.domain.Pageable.unpaged()).getTotalElements());
        ads.findAll().forEach(ad -> {
            assertTrue(ad.getTitulo().startsWith("[DEMO]"));
            assertTrue(ad.getDescricao().contains("FICTÍCIO"));
            assertNotNull(ad.getVeiculo());
            assertEquals("vendedor-demo@auto-minas.test", ad.getUsuario().getEmail());
        });

        assertEquals(480, photos.count());
        var adWithConversation = ads.findByCodigo("DEMO-001").orElseThrow();
        var conversation = new com.josenetoo_dev.veiculos_api.model.Proposta();
        conversation.setAnuncio(adWithConversation);
        conversation.setComprador(buyer);
        conversation.setValor(adWithConversation.getPreco());
        conversation.setDescricao("Conversa anterior preservada");
        conversation.setStatus(StatusProposta.PENDENTE);
        proposals.saveAndFlush(conversation);
        org.springframework.test.util.ReflectionTestUtils.setField(seeder, "presentationOwnerEmail", buyer.getEmail());
        try {
            seeder.run(new DefaultApplicationArguments(new String[0]));
            assertEquals(seller.getId(), ads.findByCodigo("DEMO-001").orElseThrow().getUsuario().getId());
            assertEquals(buyer.getId(), ads.findByCodigo("DEMO-002").orElseThrow().getUsuario().getId());
            assertEquals(1, proposals.count());
        } finally {
            org.springframework.test.util.ReflectionTestUtils.setField(seeder, "presentationOwnerEmail", "");
        }
        var secondSeller = new com.josenetoo_dev.veiculos_api.model.Usuario();
        secondSeller.setNome("Outro vendedor de teste");
        secondSeller.setEmail("segundo-vendedor@example.test");
        secondSeller.setTelefone("38000000001");
        secondSeller.setSenha(passwords.encode("temporary-test-password"));
        secondSeller.setStatus(StatusUsuario.ACTIVE);
        secondSeller.setRole(Role.USER);
        users.saveAndFlush(secondSeller);
        org.springframework.test.util.ReflectionTestUtils.setField(seeder, "secondSellerEmail", secondSeller.getEmail());
        try {
            seeder.run(new DefaultApplicationArguments(new String[0]));
            assertEquals(secondSeller.getId(), ads.findByCodigo("DEMO-151").orElseThrow().getUsuario().getId());
            assertEquals(secondSeller.getId(), ads.findByCodigo("DEMO-160").orElseThrow().getUsuario().getId());
            assertEquals(buyer.getId(), ads.findByCodigo("DEMO-002").orElseThrow().getUsuario().getId());
            seeder.run(new DefaultApplicationArguments(new String[0]));
        } finally {
            org.springframework.test.util.ReflectionTestUtils.setField(seeder, "secondSellerEmail", "");
        }
        org.springframework.test.util.ReflectionTestUtils.setField(seeder, "presentationAdminEmail", secondSeller.getEmail());
        try {
            seeder.run(new DefaultApplicationArguments(new String[0]));
            assertEquals(Role.USER, users.findById(secondSeller.getId()).orElseThrow().getRole());
            secondSeller.setContactEmailVerifiedAt(java.time.LocalDateTime.now());
            users.saveAndFlush(secondSeller);
            long previousVersion = secondSeller.getTokenVersion();
            seeder.run(new DefaultApplicationArguments(new String[0]));
            var promoted = users.findById(secondSeller.getId()).orElseThrow();
            assertEquals(Role.ADMIN, promoted.getRole());
            assertEquals(previousVersion + 1, promoted.getTokenVersion());
            seeder.run(new DefaultApplicationArguments(new String[0]));
            assertEquals(previousVersion + 1, users.findById(secondSeller.getId()).orElseThrow().getTokenVersion());
            assertEquals(secondSeller.getId(), ads.findByCodigo("DEMO-151").orElseThrow().getUsuario().getId());
        } finally {
            org.springframework.test.util.ReflectionTestUtils.setField(seeder, "presentationAdminEmail", "");
        }
        assertEquals(4, users.count());
        assertEquals(160, ads.count());
        assertEquals(480, photos.count());
    }
}
