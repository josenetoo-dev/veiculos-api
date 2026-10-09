package com.josenetoo_dev.veiculos_api;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="MYSQL_TEST_URL", matches="jdbc:mysql://127\\.0\\.0\\.1:3306/phase1_test.*")
class MySqlMigrationTest {
    static String testUrl(String purpose) {
        String base = System.getenv("MYSQL_TEST_URL");
        if (base == null || !base.startsWith("jdbc:mysql://127.0.0.1:3306/phase1_test")) throw new IllegalStateException("Somente MySQL local temporário de testes");
        return "jdbc:mysql://127.0.0.1:3306/phase1_test_"+purpose+"_"+UUID.randomUUID().toString().replace("-", "")+"?createDatabaseIfNotExist=true";
    }
    Flyway flyway(String url, String target) {return Flyway.configure().dataSource(url,"root","").locations("classpath:db/migration").target(target).load();}
    @Test void freshDatabaseGetsLegacyTablesAndSecurityColumns() throws Exception {
        String url=testUrl("fresh");var f=flyway(url,"2");assertEquals(2,f.migrate().migrationsExecuted);assertTrue(f.validateWithResult().validationSuccessful);
        try(var c=DriverManager.getConnection(url,"root","");var s=c.createStatement();var r=s.executeQuery("SELECT role,status,token_version FROM usuario")){assertFalse(r.next());}
        assertEquals(0,f.migrate().migrationsExecuted);
    }
    @Test void existingDatabaseRequiresExplicitBaselineAndPreservesData() throws Exception {
        String url=testUrl("legacy");flyway(url,"1").migrate();
        try(var c=DriverManager.getConnection(url,"root","");var s=c.createStatement()) {
            s.execute("INSERT INTO usuario(id,nome,email,senha,telefone,criado_em) VALUES(77,'Legado','legacy@example.com','test-only-password-hash','38999999999',NOW())");
            s.execute("INSERT INTO anuncio(id,codigo,versao,destaque,documentacao,garantia,titulo,descricao,preco,marca,modelo,ano,quilometragem,cor,combustivel,segunda_mao,status,cambio,categoria,criado_em,usuario_id) VALUES(88,'AM-88','LT',false,'Regular','Nenhuma','Carro','Descrição antiga',10000,'Marca','Modelo',2022,100,'Branco','FLEX',false,'ATIVO','MANUAL','SEMINOVOS',NOW(),77)");
            s.execute("INSERT INTO anuncio_foto(id,anuncio_id,url,ordem,tipo_foto) VALUES(99,88,'https://example.com/legacy.png',0,'FRENTE')");
            s.execute("DROP TABLE flyway_schema_history"); // apenas DB isolado recém-criado: simula legado sem Flyway
        }
        var f=flyway(url,"2");assertThrows(org.flywaydb.core.api.FlywayException.class,f::migrate);
        Flyway.configure().dataSource(url,"root","").baselineVersion("1").load().baseline();
        assertEquals(1,f.migrate().migrationsExecuted);
        try(var c=DriverManager.getConnection(url,"root","");var s=c.createStatement();var r=s.executeQuery("SELECT u.id,u.nome,u.email,u.senha,u.telefone,u.role,u.status,u.token_version,a.codigo,a.preco,f.url FROM usuario u JOIN anuncio a ON a.usuario_id=u.id JOIN anuncio_foto f ON f.anuncio_id=a.id")) {
            assertTrue(r.next());assertEquals(77,r.getLong(1));assertEquals("Legado",r.getString(2));assertEquals("legacy@example.com",r.getString(3));assertEquals("test-only-password-hash",r.getString(4));assertEquals("38999999999",r.getString(5));assertEquals("USER",r.getString(6));assertEquals("ACTIVE",r.getString(7));assertEquals(0,r.getLong(8));assertEquals("AM-88",r.getString(9));assertEquals(0,new java.math.BigDecimal("10000").compareTo(r.getBigDecimal(10)));assertEquals("https://example.com/legacy.png",r.getString(11));assertFalse(r.next());
        }
        assertEquals(0,f.migrate().migrationsExecuted);
    }

    @Test void pendingEmailMigrationPreservesLegacyUser() throws Exception {
        String url = testUrl("email");
        assertEquals(2, flyway(url,"2").migrate().migrationsExecuted);
        try(var c=DriverManager.getConnection(url,"root","");var st=c.createStatement()) {
            st.execute("INSERT INTO usuario(id,nome,email,senha,telefone) VALUES (101,'Original','original@example.com','unchanged-hash','38999999999')");
        }
        var upgraded = flyway(url,"3");
        assertEquals(1,upgraded.migrate().migrationsExecuted);
        assertTrue(upgraded.validateWithResult().validationSuccessful);
        try(var c=DriverManager.getConnection(url,"root","");var st=c.createStatement();
            var r=st.executeQuery("SELECT id,email,senha,pending_email,pending_email_token_hash,pending_email_expires_at,pending_email_requested_at FROM usuario WHERE id=101")) {
            assertTrue(r.next());
            assertEquals(101,r.getLong("id"));
            assertEquals("original@example.com",r.getString("email"));
            assertEquals("unchanged-hash",r.getString("senha"));
            assertNull(r.getString("pending_email"));
            assertNull(r.getString("pending_email_token_hash"));
            assertNull(r.getTimestamp("pending_email_expires_at"));
            assertNull(r.getTimestamp("pending_email_requested_at"));
        }
    }

    @Test void phase2ModerationMigrationUnpublishesLegacyActiveListingsWithoutDeletingData() throws Exception {
        String url = testUrl("moderation");
        var old = flyway(url,"3");
        assertEquals(3,old.migrate().migrationsExecuted);
        try(var c=DriverManager.getConnection(url,"root",""); var stmt=c.createStatement()) {
            stmt.execute("INSERT INTO usuario(id,nome,email,senha,telefone) VALUES (201,'Legacy','legacy2@example.com','hash','38999999999')");
            String prefix = "INSERT INTO anuncio(id,codigo,versao,destaque,documentacao,garantia,titulo,descricao,preco,marca,modelo,ano,quilometragem,cor,combustivel,segunda_mao,status,cambio,categoria,criado_em,usuario_id) VALUES ";
            stmt.execute(prefix+"(301,'AM-301','LT',false,'Regular','Nenhuma','Ativo','Descricao',10000,'Marca','Modelo',2022,100,'Branco','FLEX',false,'ATIVO','MANUAL','SEMINOVOS',NOW(),201)");
            stmt.execute(prefix+"(302,'AM-302','LT',false,'Regular','Nenhuma','Pausado','Descricao',10000,'Marca','Modelo',2022,100,'Branco','FLEX',false,'PAUSADO','MANUAL','SEMINOVOS',NOW(),201)");
        }
        var migrated = flyway(url,"4");
        assertEquals(1,migrated.migrate().migrationsExecuted);
        assertTrue(migrated.validateWithResult().validationSuccessful);
        try(var c=DriverManager.getConnection(url,"root",""); var stmt=c.createStatement();
            var rows=stmt.executeQuery("SELECT id,status,revisado_em,revisado_por_id,motivo_rejeicao FROM anuncio ORDER BY id")) {
            assertTrue(rows.next()); assertEquals(301,rows.getLong(1)); assertEquals("PENDENTE",rows.getString(2));
            assertNull(rows.getTimestamp(3));assertNull(rows.getObject(4));assertNull(rows.getString(5));
            assertTrue(rows.next()); assertEquals(302,rows.getLong(1)); assertEquals("PAUSADO",rows.getString(2));
            assertFalse(rows.next());
        }
        try(var c=DriverManager.getConnection(url,"root",""); var stmt=c.createStatement();
            var logs=stmt.executeQuery("SELECT COUNT(*) FROM moderacao_evento")) {
            assertTrue(logs.next());assertEquals(0,logs.getInt(1));
        }
        assertEquals(0,migrated.migrate().migrationsExecuted);
    }

}
