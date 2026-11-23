package com.josenetoo_dev.veiculos_api;

import com.josenetoo_dev.veiculos_api.security.AbuseRateStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MySqlTestEnabledCondition.class)
class AbuseRateStoreMySqlTest {
    @Test
    void sharedDatabaseSerializesConcurrentRequestsAndSeparatesIdentities() throws Exception {
        String url = MySqlMigrationTest.testUrl("abuse_window");
        MySqlMigrationTest.flyway(url, "1").migrate();
        MySqlMigrationTest.flyway(url).migrate();
        var config = MySqlTestConfiguration.fromEnvironment();
        var source = new DriverManagerDataSource(url, config.user(), config.password());
        var db = new JdbcTemplate(source);
        var transactions = new TransactionTemplate(new DataSourceTransactionManager(source));
        var store = new AbuseRateStore(db, transactions, "test-secret-with-at-least-thirty-two-bytes");
        try (var pool = Executors.newFixedThreadPool(8)) {
            var tasks = new ArrayList<Callable<Long>>();
            for (int i = 0; i < 16; i++) tasks.add(() -> store.consume("user:auth:123", 5, 60));
            var results = pool.invokeAll(tasks);
            int accepted = 0;
            for (var result : results) if (result.get(20, TimeUnit.SECONDS) == 0) accepted++;
            assertEquals(5, accepted);
        }
        assertTrue(store.consume("user:auth:123", 5, 60) > 0);
        assertEquals(0, store.consume("user:auth:456", 5, 60));
        assertEquals(0, store.consume("ip:auth:shared", 80, 60));
        assertEquals(7, db.queryForObject("SELECT COUNT(*) FROM abuse_rate_event", Integer.class));
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM abuse_rate_key WHERE key_hash LIKE '%shared%'", Integer.class));
    }
}
