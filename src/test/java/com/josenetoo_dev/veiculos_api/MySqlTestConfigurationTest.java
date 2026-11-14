package com.josenetoo_dev.veiculos_api;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MySqlTestConfigurationTest {
    private static Map<String, String> validEnvironment() {
        return new HashMap<>(Map.of(
                "MYSQL_TEST_URL", "jdbc:mysql://127.0.0.1:13306/phase1_test",
                "MYSQL_TEST_USER", "migration_test",
                "MYSQL_TEST_PASSWORD", "test-only-password"));
    }

    @Test
    void acceptsLoopbackEndpointAndUsesConfiguredPortAndCredentials() {
        var configuration = MySqlTestConfiguration.from(validEnvironment());

        assertEquals("127.0.0.1", configuration.host());
        assertEquals(13306, configuration.port());
        assertEquals("migration_test", configuration.user());
        assertEquals("test-only-password", configuration.password());
    }

    @Test
    void generatesUniqueSchemasAndPreservesConfiguredEndpoint() {
        var configuration = MySqlTestConfiguration.from(validEnvironment());

        String first = configuration.schemaUrl("migration");
        String second = configuration.schemaUrl("migration");

        assertNotEquals(first, second);
        assertTrue(first.matches("jdbc:mysql://127\\.0\\.0\\.1:13306/phase1_test_migration_[a-f0-9]{32}\\?createDatabaseIfNotExist=true"));
    }

    @Test
    void rejectsMissingOrBlankConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> MySqlTestConfiguration.from(Map.of()));
        assertTrue(MySqlTestConfiguration.isCompletelyAbsent(Map.of()));
        var missingPassword = validEnvironment();
        missingPassword.remove("MYSQL_TEST_PASSWORD");
        assertThrows(IllegalArgumentException.class, () -> MySqlTestConfiguration.from(missingPassword));
        assertFalse(MySqlTestConfiguration.isCompletelyAbsent(missingPassword));
        var blankUser = validEnvironment();
        blankUser.put("MYSQL_TEST_USER", "  ");
        assertThrows(IllegalArgumentException.class, () -> MySqlTestConfiguration.from(blankUser));
    }

    @Test
    void rejectsNonLoopbackHostsAndNonTestSchemas() {
        assertRejectedUrl("jdbc:mysql://localhost:13306/phase1_test");
        assertRejectedUrl("jdbc:mysql://192.168.1.20:13306/phase1_test");
        assertRejectedUrl("jdbc:mysql://127.0.0.1:13306/auto_minas_staging");
        assertRejectedUrl("jdbc:mysql://127.0.0.1:13306/auto_minas");
    }

    @Test
    void rejectsProductionPortInvalidPortsAndJdbcOverrides() {
        assertRejectedUrl("jdbc:mysql://127.0.0.1:3306/phase1_test");
        assertRejectedUrl("jdbc:mysql://127.0.0.1:0/phase1_test");
        assertRejectedUrl("jdbc:mysql://127.0.0.1:65536/phase1_test");
        assertRejectedUrl("jdbc:mysql://127.0.0.1:13306/phase1_test?socketFactory=unsafe");
    }

    @Test
    void rejectsInvalidSchemaPurpose() {
        var configuration = MySqlTestConfiguration.from(validEnvironment());
        assertThrows(IllegalArgumentException.class, () -> configuration.schemaUrl("../staging"));
    }

    private static void assertRejectedUrl(String url) {
        var environment = validEnvironment();
        environment.put("MYSQL_TEST_URL", url);
        assertThrows(IllegalArgumentException.class, () -> MySqlTestConfiguration.from(environment));
    }
}
