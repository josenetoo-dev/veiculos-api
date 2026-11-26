package com.josenetoo_dev.veiculos_api;

import com.josenetoo_dev.veiculos_api.controller.FavoritoController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import java.security.Principal;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MySqlTestEnabledCondition.class)
class FavoritoMySqlTest {
    @Test void ownerIsolationDuplicateAndAvailability() {
        var url=MySqlMigrationTest.testUrl("favorites");
        MySqlMigrationTest.flyway(url).migrate();
        var config=MySqlTestConfiguration.fromEnvironment();
        var source=new DriverManagerDataSource(url,config.user(),config.password());
        var db=new JdbcTemplate(source);
        db.update("INSERT INTO usuario(id,nome,email,senha,telefone) VALUES(901,'A','favorite-a@example.test','hash','39000000000'),(902,'B','favorite-b@example.test','hash','39000000001')");
        db.update("""
            INSERT INTO veiculo(id,cadastrado_por_id,marca,modelo,versao,ano,quilometragem,cor,combustivel,cambio,segunda_mao)
            VALUES (901,901,'Fiat','Argo','LT',2022,100,'Branco','FLEX','MANUAL',false),
                   (902,901,'Fiat','Argo','LT',2022,100,'Branco','FLEX','MANUAL',false)
            """);
        db.update("""
            INSERT INTO anuncio(id,codigo,versao,destaque,documentacao,garantia,titulo,descricao,preco,
              marca,modelo,ano,quilometragem,cor,combustivel,segunda_mao,status,cambio,categoria,criado_em,usuario_id,veiculo_id)
            VALUES (901,'AM-FAV-901','LT',false,'Declarada','Nenhuma','Carro','Teste',10000,
              'Fiat','Argo',2022,100,'Branco','FLEX',false,'ATIVO','MANUAL','SEMINOVOS',NOW(),901,901),
              (902,'DEMO-FAV-902','LT',false,'Declarada','Nenhuma','Demo','Teste',10000,
              'Fiat','Argo',2022,100,'Branco','FLEX',false,'ATIVO','MANUAL','SEMINOVOS',NOW(),901,902)
            """);
        var controller=new FavoritoController(db);
        Principal a=()->"901", b=()->"902";
        var tx=new TransactionTemplate(new DataSourceTransactionManager(source));
        tx.executeWithoutResult(s->{controller.adicionar(a,901);controller.adicionar(a,901);});
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM anuncio_favorito",Integer.class));
        assertEquals(java.util.List.of(901L),controller.meus(a));
        assertTrue(controller.meus(b).isEmpty());
        tx.executeWithoutResult(s->controller.adicionar(a,902));
        assertEquals(java.util.List.of(902L,901L),controller.meus(a));
        controller.remover(b,901);
        assertTrue(controller.existe(a,901));
        controller.remover(a,901);
        assertFalse(controller.existe(a,901));
    }
}
