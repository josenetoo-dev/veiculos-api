package com.josenetoo_dev.veiculos_api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MySqlTestEnabledCondition.class)
class ProOfferMySqlTest {
    @Test
    void migrationPreservesLegacyRowsAndLeavesProUnpriced() {
        String url = MySqlMigrationTest.testUrl("pro_offer");
        MySqlMigrationTest.flyway(url).migrate();
        var config = MySqlTestConfiguration.fromEnvironment();
        var db = new JdbcTemplate(new DriverManagerDataSource(url,config.user(),config.password()));
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM plano_comercial WHERE ativo=TRUE", Integer.class));
        assertEquals("AUTO_MINAS_PRO", db.queryForObject("SELECT codigo FROM plano_comercial WHERE ativo=TRUE", String.class));
        assertNull(db.queryForObject("SELECT mensalidade FROM plano_comercial WHERE codigo='AUTO_MINAS_PRO'", java.math.BigDecimal.class));
        assertEquals(5, db.queryForObject("SELECT COUNT(*) FROM plano_comercial WHERE ativo=FALSE", Integer.class));
        MySqlMigrationTest.flyway(url).migrate();
        assertEquals(6, db.queryForObject("SELECT COUNT(*) FROM plano_comercial", Integer.class));
    }
}
