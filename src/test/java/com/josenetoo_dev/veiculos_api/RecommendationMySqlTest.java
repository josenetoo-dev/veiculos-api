package com.josenetoo_dev.veiculos_api;

import com.josenetoo_dev.veiculos_api.recommendation.application.Signals;
import com.josenetoo_dev.veiculos_api.recommendation.infrastructure.Catalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MySqlTestEnabledCondition.class)
class RecommendationMySqlTest {
    @Test
    void inventoryBenchmarkAndConcurrentDeduplication() throws Exception {
        String url = MySqlMigrationTest.testUrl("recommendation");
        MySqlMigrationTest.flyway(url, "1").migrate();
        var configuration = MySqlTestConfiguration.fromEnvironment();
        var source = new DriverManagerDataSource(url, configuration.user(), configuration.password());
        var db = new JdbcTemplate(source);
        db.update("""
            INSERT INTO usuario(id,nome,email,senha,telefone)
            VALUES(1,'Benchmark','benchmark@example.com','test-only-hash','00000000000')
            """);
        String digits = "(SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3"
                + " UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7"
                + " UNION ALL SELECT 8 UNION ALL SELECT 9)";
        db.update("""
            INSERT INTO anuncio(id,codigo,versao,destaque,documentacao,garantia,titulo,descricao,
                preco,marca,modelo,ano,quilometragem,cor,combustivel,segunda_mao,status,cambio,
                categoria,criado_em,usuario_id)
            SELECT n,CONCAT('BENCH-',n),'Base',FALSE,'Regular','Nenhuma','Teste','Fixture',
                10000+MOD(n,100)*1000,CONCAT('Marca',MOD(n,10)),CONCAT('Modelo',MOD(n,50)),
                2022,100,'Branco','FLEX',FALSE,'ATIVO','MANUAL','SEMINOVOS',
                TIMESTAMPADD(SECOND,n,'2026-01-01 00:00:00'),1
            FROM (SELECT 1+a.n+10*b.n+100*c.n+1000*d.n n FROM
            """ + digits+" a CROSS JOIN "+digits+" b CROSS JOIN "+digits+" c CROSS JOIN "+digits+" d) fixture");
        MySqlMigrationTest.flyway(url).migrate();
        db.update("UPDATE anuncio SET status='ATIVO',uf=IF(MOD(id,2)=0,'MG','SP')");
        db.update("""
            INSERT INTO proposta(valor,descricao,status,criado_em,contraproposta_feita,anunciante_id,comprador_id)
            SELECT preco,'Fixture','PENDENTE',criado_em,FALSE,id,1 FROM anuncio
            """);
        var catalog = new Catalog(JdbcClient.create(source));
        for (int i=0; i<5; i++) catalog.load(null,null,null,false);
        var durations = new ArrayList<Double>();
        for (int i=0; i<30; i++) {
            long start = System.nanoTime();
            var inventory = catalog.load(BigDecimal.valueOf(20000),BigDecimal.valueOf(60000),"MG",false);
            durations.add((System.nanoTime()-start)/1_000_000d);
            assertEquals(240,inventory.size());
            assertTrue(inventory.stream().allMatch(a -> a.uf().equals("MG")
                    && a.preco().compareTo(BigDecimal.valueOf(20000))>=0
                    && a.preco().compareTo(BigDecimal.valueOf(60000))<=0));
        }
        Collections.sort(durations);
        System.out.printf(Locale.ROOT,"RECOMMENDATION_BENCHMARK rows=10000 proposals=10000 samples=30 p50_ms=%.3f p95_ms=%.3f%n",
                durations.get(14),durations.get(28));
        var plan = db.queryForList("""
            EXPLAIN SELECT id FROM anuncio WHERE status='ATIVO'
            ORDER BY criado_em DESC,id DESC LIMIT 240
            """);
        assertTrue(plan.stream().anyMatch(row -> "ix_rec_inventory".equals(row.get("key"))));
        System.out.println("RECOMMENDATION_INVENTORY_PLAN " + plan);
        var signals = new Signals(db,new MockEnvironment());
        var transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        String session = UUID.randomUUID().toString();
        var event = new Signals.Event(UUID.randomUUID(),Signals.Kind.DETAILS,10000L,null,null,true);
        try (var executor = Executors.newFixedThreadPool(8)) {
            var tasks = new ArrayList<Callable<Void>>();
            for (int i=0; i<16; i++) tasks.add(() -> {
                transaction.executeWithoutResult(status -> signals.record(event,session,1L));
                return null;
            });
            for (var result : executor.invokeAll(tasks)) result.get(20,TimeUnit.SECONDS);
        }
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM recommendation_signal",Integer.class));
        db.update("""
            INSERT INTO usuario(id,nome,email,senha,telefone)
            VALUES(2,'Second','second@example.com','test-only-hash','00000000000')
            """);
        transaction.executeWithoutResult(status -> signals.record(event,session,2L));
        assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM recommendation_signal",Integer.class));
        transaction.executeWithoutResult(status -> signals.delete(1));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM recommendation_signal",Integer.class));
        transaction.executeWithoutResult(status -> signals.delete(2));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM recommendation_signal",Integer.class));
    }
}
