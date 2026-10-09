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


    @Test void vehicleMigrationBackfillsLegacyRowsWithoutLosingOffersOrPhotos() throws Exception {
        String url = testUrl("vehicle");
        var stage4 = flyway(url,"4");
        assertEquals(4,stage4.migrate().migrationsExecuted);
        try(var c=DriverManager.getConnection(url,"root",""); var st=c.createStatement()) {
            st.execute("INSERT INTO usuario(id,nome,email,senha,telefone) VALUES (401,'Legacy','vehicle@example.com','old-hash','38999999999')");
            st.execute("INSERT INTO anuncio(id,codigo,versao,destaque,documentacao,garantia,titulo,descricao,preco,marca,modelo,ano,quilometragem,cor,combustivel,segunda_mao,status,cambio,categoria,criado_em,usuario_id) VALUES (501,'AM-501','LT',false,'Regular','Nenhuma','Legacy Listing','Original',59000,'Chevrolet','Onix',2022,58100,'Branco','FLEX',true,'PENDENTE','MANUAL','SEMINOVOS',NOW(),401)");
            st.execute("INSERT INTO anuncio_foto(id,anuncio_id,url,ordem,tipo_foto) VALUES (601,501,'https://example.com/legacy.png',0,'FRENTE')");
            st.execute("INSERT INTO proposta(id,valor,descricao,status,criado_em,contraproposta_feita,anunciante_id,comprador_id) VALUES (701,59000,'Proposta','PENDENTE',NOW(),false,501,401)");
        }
        var migrated = flyway(url,"5");
        assertEquals(1,migrated.migrate().migrationsExecuted);
        assertTrue(migrated.validateWithResult().validationSuccessful);
        try(var c=DriverManager.getConnection(url,"root","");var st=c.createStatement();
            var rows=st.executeQuery("SELECT a.id,a.veiculo_id,a.codigo,a.marca,a.preco,a.status,v.id,v.marca,v.modelo,v.versao,v.ano,v.quilometragem,v.combustivel,v.cambio,v.cadastrado_por_id FROM anuncio a JOIN veiculo v ON v.id=a.veiculo_id WHERE a.id=501")) {
            assertTrue(rows.next());
            assertEquals(501,rows.getLong("id"));
            assertEquals(501,rows.getLong("veiculo_id"));
            assertEquals("AM-501",rows.getString("codigo"));
            assertEquals("Chevrolet",rows.getString("marca"));
            assertEquals(0,new java.math.BigDecimal("59000").compareTo(rows.getBigDecimal("preco")));
            assertEquals("PENDENTE",rows.getString("status"));
            assertEquals(501,rows.getLong(7));
            assertEquals("Chevrolet",rows.getString(8));
            assertEquals("Onix",rows.getString(9));
            assertEquals("LT",rows.getString(10));
            assertEquals(2022,rows.getInt(11));
            assertEquals(58100,rows.getInt(12));
            assertEquals("FLEX",rows.getString(13));
            assertEquals("MANUAL",rows.getString(14));
            assertEquals(401,rows.getLong(15));
            assertFalse(rows.next());
        }
        try(var c=DriverManager.getConnection(url,"root","");var st=c.createStatement();
            var rows=st.executeQuery("SELECT p.id,p.anunciante_id,f.anuncio_id,f.url FROM proposta p JOIN anuncio_foto f ON f.anuncio_id=p.anunciante_id WHERE p.id=701")) {
            assertTrue(rows.next());
            assertEquals(701,rows.getLong(1));
            assertEquals(501,rows.getLong(2));
            assertEquals(501,rows.getLong(3));
            assertEquals("https://example.com/legacy.png",rows.getString(4));
        }
        assertEquals(0,migrated.migrate().migrationsExecuted);
    }

    @Test void verificationTablesAreAddedWithoutTouchingLegacyRecords() throws Exception {
        String url=testUrl("verification");
        assertEquals(5,flyway(url,"5").migrate().migrationsExecuted);
        try(var c=DriverManager.getConnection(url,"root","");var st=c.createStatement()) {
            st.execute("INSERT INTO usuario(id,nome,email,senha,telefone) VALUES (401,'Legacy','verification@example.com','old-hash','38999999999')");
            st.execute("INSERT INTO veiculo(id,cadastrado_por_id,marca,modelo,versao,ano,quilometragem,cor,combustivel,cambio,segunda_mao) VALUES (501,401,'Chevrolet','Onix','LT',2022,60000,'Branco','FLEX','MANUAL',true)");
        }
        var migrated=flyway(url,"6");
        assertEquals(1,migrated.migrate().migrationsExecuted);
        assertTrue(migrated.validateWithResult().validationSuccessful);
        try(var c=DriverManager.getConnection(url,"root",""); var stmt=c.createStatement()) {
            stmt.execute("INSERT INTO verificacao (id,solicitante_id,usuario_identidade_id,tipo,status,criado_em) VALUES (601,401,401,'IDENTIDADE','EM_ANALISE',NOW())");
            stmt.execute("INSERT INTO evidencia_verificacao (id,verificacao_id,tipo,arquivo_chave,tipo_midia,tamanho,criado_em) VALUES (701,601,'IDENTIDADE_FRENTE','00000000-0000-4000-8000-000000000001','image/png',100,NOW())");
            stmt.execute("INSERT INTO evento_verificacao (id,verificacao_id,revisor_id,resultado,criado_em) VALUES (801,601,401,'APROVADA',NOW())");
            try(var r=stmt.executeQuery("SELECT email FROM usuario WHERE id=401")) {
                assertTrue(r.next());assertEquals("verification@example.com",r.getString(1));
            }
            try(var r=stmt.executeQuery("SELECT COUNT(*) FROM veiculo WHERE id=501")) {
                assertTrue(r.next());assertEquals(1,r.getLong(1));
            }
            try(var r=stmt.executeQuery("SELECT COUNT(*) FROM evidencia_verificacao WHERE verificacao_id=601")) {
                assertTrue(r.next());assertEquals(1,r.getLong(1));
            }
            assertThrows(SQLException.class,()->stmt.execute(
                "INSERT INTO verificacao (solicitante_id,usuario_identidade_id,veiculo_id,tipo,status,criado_em) VALUES (401,401,501,'IDENTIDADE','RASCUNHO',NOW())"));
        }
        assertEquals(0,migrated.migrate().migrationsExecuted);
    }

    @Test void contactEmailMigrationKeepsExistingUsersAndPasswords() throws Exception {
        String url=testUrl("contact");
        assertEquals(6,flyway(url,"6").migrate().migrationsExecuted);
        try(var c=DriverManager.getConnection(url,"root","");var st=c.createStatement()) {
            st.execute("INSERT INTO usuario(id,nome,email,senha,telefone,role,status,token_version) VALUES (902,'Legacy','contact@example.com','old-hash','38999999999','USER','ACTIVE',3)");
        }
        var upgraded=flyway(url,"7");
        assertEquals(1,upgraded.migrate().migrationsExecuted);
        assertTrue(upgraded.validateWithResult().validationSuccessful);
        try(var c=DriverManager.getConnection(url,"root","");var st=c.createStatement();
             var r=st.executeQuery("SELECT email,senha,status,token_version,contact_email_token_hash,contact_email_expires_at,contact_email_requested_at,contact_email_verified_at FROM usuario WHERE id=902")) {
            assertTrue(r.next());
            assertEquals("contact@example.com",r.getString("email"));
            assertEquals("old-hash",r.getString("senha"));
            assertEquals("ACTIVE",r.getString("status"));
            assertEquals(3,r.getLong("token_version"));
            assertNull(r.getString("contact_email_token_hash"));
            assertNull(r.getTimestamp("contact_email_expires_at"));
            assertNull(r.getTimestamp("contact_email_requested_at"));
            assertNull(r.getTimestamp("contact_email_verified_at"));
        }
        assertEquals(0,upgraded.migrate().migrationsExecuted);
    }

    @Test void archiveMigrationAddsColumnsWithoutDeletingOffersOrMessages() throws Exception {
        String url=testUrl("archiving");
        assertEquals(7,flyway(url,"7").migrate().migrationsExecuted);
        try(var c=DriverManager.getConnection(url,"root","");var st=c.createStatement()) {
            st.execute("INSERT INTO usuario(id,nome,email,senha,telefone,role,status,token_version) VALUES (801,'Legacy','archive@example.com','hash','38999999999','USER','ACTIVE',0)");
            st.execute("INSERT INTO veiculo(id,cadastrado_por_id,marca,modelo,versao,ano,quilometragem,cor,combustivel,cambio,segunda_mao) VALUES (802,801,'Chevrolet','Onix','LT',2022,100,'Branco','FLEX','MANUAL',true)");
            st.execute("INSERT INTO anuncio(id,codigo,versao,destaque,documentacao,garantia,titulo,descricao,preco,marca,modelo,ano,quilometragem,cor,combustivel,segunda_mao,status,cambio,categoria,criado_em,usuario_id,veiculo_id) VALUES (803,'AM-803','LT',false,'Regular','Nenhuma','Carro','Descricao',60000,'Chevrolet','Onix',2022,100,'Branco','FLEX',true,'PENDENTE','MANUAL','SEMINOVOS',NOW(),801,802)");
            st.execute("INSERT INTO proposta(id,valor,descricao,status,criado_em,contraproposta_feita,anunciante_id,comprador_id) VALUES (804,59000,'Proposta','PENDENTE',NOW(),false,803,801)");
            st.execute("INSERT INTO mensagem(id,conteudo,criado_em,proposta_id,remetente_id) VALUES (805,'Negociacao',NOW(),804,801)");
        }
        var m=flyway(url,"8");
        assertEquals(1,m.migrate().migrationsExecuted);
        assertTrue(m.validateWithResult().validationSuccessful);
        try(var c=DriverManager.getConnection(url,"root","");var st=c.createStatement()) {
            try(var rs=st.executeQuery("SELECT id,arquivado_em,arquivado_por_id FROM anuncio WHERE id=803")) {
                assertTrue(rs.next());assertEquals(803,rs.getLong(1));assertNull(rs.getTimestamp(2));assertNull(rs.getObject(3));
            }
            try(var rs=st.executeQuery("SELECT p.id,m.id FROM proposta p JOIN mensagem m ON m.proposta_id=p.id WHERE p.anunciante_id=803")) {
                assertTrue(rs.next());assertEquals(804,rs.getLong(1));assertEquals(805,rs.getLong(2));
            }
        }
        assertEquals(0,m.migrate().migrationsExecuted);
    }


    @Test void listingReportsMigrationPreservesLegacyAdsAndEnforcesUniqueness() throws Exception {
        String url=testUrl("reports");
        assertEquals(8,flyway(url,"8").migrate().migrationsExecuted);
        try(var c=DriverManager.getConnection(url,"root",""); var st=c.createStatement()) {
            st.execute("INSERT INTO usuario(id,nome,email,senha,telefone,role,status,token_version) VALUES (901,'Seller','seller@example.com','old-hash','38999999999','USER','ACTIVE',0)");
            st.execute("INSERT INTO usuario(id,nome,email,senha,telefone,role,status,token_version) VALUES (902,'Buyer','buyer@example.com','old-hash','38999999998','USER','ACTIVE',0)");
            st.execute("INSERT INTO veiculo(id,cadastrado_por_id,marca,modelo,versao,ano,quilometragem,cor,combustivel,cambio,segunda_mao) VALUES (903,901,'Chevrolet','Onix','LT',2022,100,'Branco','FLEX','MANUAL',true)");
            st.execute("INSERT INTO anuncio(id,codigo,versao,destaque,documentacao,garantia,titulo,descricao,preco,marca,modelo,ano,quilometragem,cor,combustivel,segunda_mao,status,cambio,categoria,criado_em,usuario_id,veiculo_id) VALUES (904,'AM-904','LT',false,'Regular','Nenhuma','Carro','Descricao',59000,'Chevrolet','Onix',2022,100,'Branco','FLEX',true,'ATIVO','MANUAL','SEMINOVOS',NOW(),901,903)");
        }
        var m=flyway(url,"9");
        assertEquals(1,m.migrate().migrationsExecuted);
        assertTrue(m.validateWithResult().validationSuccessful);
        try(var c=DriverManager.getConnection(url,"root",""); var st=c.createStatement()) {
            st.execute("INSERT INTO denuncia(anuncio_id,denunciante_id,categoria,relato,status,criado_em) VALUES (904,902,'POSSIVEL_FRAUDE','Relato de fraude','ABERTA',NOW())");
            assertThrows(SQLException.class,()->st.execute("INSERT INTO denuncia(anuncio_id,denunciante_id,categoria,relato,status,criado_em) VALUES (904,902,'OUTRO','Duplicada','ABERTA',NOW())"));
            try(var rs=st.executeQuery("SELECT status FROM anuncio WHERE id=904")) {
                assertTrue(rs.next());assertEquals("ATIVO",rs.getString(1));
            }
            try(var rs=st.executeQuery("SELECT COUNT(*) FROM denuncia")) {
                assertTrue(rs.next());assertEquals(1,rs.getLong(1));
            }
        }
        assertEquals(0,m.migrate().migrationsExecuted);
    }

}
