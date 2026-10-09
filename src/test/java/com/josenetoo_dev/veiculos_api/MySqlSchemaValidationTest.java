package com.josenetoo_dev.veiculos_api;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.enums.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="MYSQL_TEST_URL",matches="jdbc:mysql://127\\.0\\.0\\.1:3306/phase1_test.*")
@SpringBootTest(properties={"spring.datasource.username=root","spring.datasource.password=",
 "spring.flyway.enabled=true","spring.jpa.hibernate.ddl-auto=validate","spring.jpa.show-sql=false",
 "jwt.secret=mysql-test-only-key-at-least-32-bytes-long"})
class MySqlSchemaValidationTest {
 static final String URL=System.getenv("MYSQL_TEST_URL") == null ? "" : MySqlMigrationTest.testUrl("jpa");
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",()->URL);}
 @Autowired UsuarioRepository usuarios;
 @Test void flywaySchemaIsCompatibleWithHibernateAndSecurityFieldsPersist(){
  var u=new Usuario();u.setNome("Test");u.setEmail("test@example.com");u.setSenha("test-only-hash");u.setTelefone("38999999999");
  u=usuarios.saveAndFlush(u);var saved=usuarios.findById(u.getId()).orElseThrow();
  assertEquals(Role.USER,saved.getRole());assertEquals(StatusUsuario.PENDING_CONTACT_VERIFICATION,saved.getStatus());assertEquals(0,saved.getTokenVersion());
 }
}
