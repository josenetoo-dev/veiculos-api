package com.josenetoo_dev.veiculos_api;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.enums.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MySqlTestEnabledCondition.class)
@SpringBootTest(properties={"spring.flyway.enabled=true","spring.jpa.hibernate.ddl-auto=validate","spring.jpa.show-sql=false",
 "jwt.secret=mysql-test-only-key-at-least-32-bytes-long"})
class MySqlSchemaValidationTest {
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){
  var configuration=MySqlTestConfiguration.fromEnvironment();
  String url=configuration.schemaUrl("jpa");
  r.add("spring.datasource.url",()->url);
  r.add("spring.datasource.username",configuration::user);
  r.add("spring.datasource.password",configuration::password);
 }
 @Autowired UsuarioRepository usuarios;
 @Test void flywaySchemaIsCompatibleWithHibernateAndSecurityFieldsPersist(){
  var u=new Usuario();u.setNome("Test");u.setEmail("test@example.com");u.setSenha("test-only-hash");u.setTelefone("38999999999");
  u=usuarios.saveAndFlush(u);var saved=usuarios.findById(u.getId()).orElseThrow();
  assertEquals(Role.USER,saved.getRole());assertEquals(StatusUsuario.PENDING_CONTACT_VERIFICATION,saved.getStatus());assertEquals(0,saved.getTokenVersion());
 }
}
