package com.josenetoo_dev.veiculos_api;

import com.josenetoo_dev.veiculos_api.controller.BuscaSalvaController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import java.security.Principal;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MySqlTestEnabledCondition.class)
class BuscaSalvaMySqlTest {
    @Test void ownerIsolationEditsAndInputValidation() {
        var url=MySqlMigrationTest.testUrl("saved_searches");
        MySqlMigrationTest.flyway(url).migrate();
        var config=MySqlTestConfiguration.fromEnvironment();
        var source=new DriverManagerDataSource(url,config.user(),config.password());
        var db=new JdbcTemplate(source);
        db.update("INSERT INTO usuario(id,nome,email,senha,telefone) VALUES(971,'A','search-a@example.test','hash','39000000000'),(972,'B','search-b@example.test','hash','39000000001')");
        var controller=new BuscaSalvaController(db);
        Principal a=()->"971", b=()->"972";
        var original=new BuscaSalvaController.Input("Até 70 mil",Map.of("maxPrice","70000","state","MG"));
        var tx=new TransactionTemplate(new DataSourceTransactionManager(source));
        var saved=tx.execute(status->controller.criar(a,original));
        assertEquals(original.filtros(),controller.minhas(a).getFirst().filtros());
        assertTrue(controller.minhas(b).isEmpty());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                ()->controller.editar(b,saved.id(),new BuscaSalvaController.Input("Inválido",Map.of("q","Fiat"))));
        var renamed=controller.editar(a,saved.id(),new BuscaSalvaController.Input("Fiat em MG",Map.of("q","Fiat","state","MG")));
        assertEquals("Fiat em MG",renamed.nome());
        controller.remover(b,saved.id());
        assertEquals(1,controller.minhas(a).size());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                ()->controller.criar(a,new BuscaSalvaController.Input("Escape",Map.of("sort","injetado"))));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                ()->controller.criar(a,new BuscaSalvaController.Input("Inversa",Map.of("minPrice","90000","maxPrice","10000"))));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                ()->controller.criar(a,new BuscaSalvaController.Input("Combustível",Map.of("fuel","inexistente"))));
        controller.remover(a,saved.id());
        assertTrue(controller.minhas(a).isEmpty());
        var fuel=tx.execute(status->controller.criar(a,new BuscaSalvaController.Input("Flex",Map.of("fuel","flex"))));
        assertEquals("flex",controller.minhas(a).getFirst().filtros().get("fuel"));
        controller.remover(a,fuel.id());
        for (int i=0;i<20;i++) db.update("INSERT INTO busca_salva(usuario_id,nome,filtros_json) VALUES(971,?,?)",
                "Busca " + i,"{\"state\":\"MG\"}");
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                ()->tx.executeWithoutResult(status->controller.criar(a,original)));
        assertEquals(20,controller.minhas(a).size());
    }
}
