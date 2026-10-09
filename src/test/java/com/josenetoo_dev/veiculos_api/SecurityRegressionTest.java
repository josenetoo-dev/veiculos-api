package com.josenetoo_dev.veiculos_api;

import com.josenetoo_dev.veiculos_api.enums.*;
import com.josenetoo_dev.veiculos_api.model.*;
import com.josenetoo_dev.veiculos_api.repository.*;
import com.josenetoo_dev.veiculos_api.security.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.nio.file.*;
import java.math.BigDecimal;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
 "spring.datasource.url=jdbc:h2:mem:security;MODE=MySQL;DB_CLOSE_DELAY=-1",
 "spring.datasource.username=sa", "spring.datasource.password=",
 "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
 "spring.flyway.enabled=false", "jwt.secret=integration-test-key-at-least-32-bytes-long",
 "logging.level.org.springframework.security=INFO"
})
@AutoConfigureMockMvc(printOnlyOnFailure = false, print = org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint.NONE)
class SecurityRegressionTest {
 static final Path UPLOAD;
 static { try { UPLOAD = Files.createTempDirectory("auto-minas-security-"); } catch(Exception e) {throw new RuntimeException(e);} }
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r) { r.add("upload.dir", () -> UPLOAD.toString()); }
 @Autowired MockMvc mvc;
 @Autowired UsuarioRepository usuarios;
 @Autowired AnuncioRepository anuncios;
 @Autowired AnuncioFotoRepository fotos;
 @Autowired PropostaRepository propostas;
 @Autowired MensagemRepository mensagens;
 @Autowired JwtUtil jwt;
 @Autowired PasswordEncoder encoder;
 Usuario a, b, c;
 Anuncio anuncio;
 Proposta proposta;
 String tokenA, tokenB, tokenC;
 @BeforeEach void setup() throws Exception {
  mensagens.deleteAll(); propostas.deleteAll(); fotos.deleteAll(); anuncios.deleteAll(); usuarios.deleteAll();
  try(var files=Files.list(UPLOAD)){ for(Path f:files.toList()) Files.delete(f); }
  a=user("A", "a@example.com"); b=user("B", "b@example.com"); c=user("C", "c@example.com");
  tokenA=jwt.gerarToken(a.getId().toString()); tokenB=jwt.gerarToken(b.getId().toString()); tokenC=jwt.gerarToken(c.getId().toString());
  anuncio=new Anuncio(); anuncio.setUsuario(b); anuncio.setVersao("LT"); anuncio.setDocumentacao("Regular"); anuncio.setGarantia("Nenhuma");
  anuncio.setTitulo("Onix"); anuncio.setDescricao("Carro"); anuncio.setPreco(new BigDecimal("50000")); anuncio.setMarca("Chevrolet"); anuncio.setModelo("Onix"); anuncio.setAno(2022); anuncio.setCor("Branco");
  anuncio.setCombustivel(TipoCombustivel.values()[0]); anuncio.setCambio(Cambio.values()[0]); anuncio.setCategoria(Categoria.values()[0]); anuncio.setStatus(StatusAnuncio.ATIVO);
  anuncio=anuncios.saveAndFlush(anuncio);
  proposta=new Proposta(); proposta.setAnuncio(anuncio); proposta.setComprador(a); proposta.setValor(BigDecimal.TEN); proposta.setDescricao("Oferta"); proposta.setStatus(StatusProposta.PENDENTE); proposta=propostas.saveAndFlush(proposta);
 }
 Usuario user(String nome,String email){ Usuario u=new Usuario(); u.setNome(nome);u.setEmail(email);u.setTelefone("38999999999");u.setSenha(encoder.encode("senhaTeste123"));return usuarios.saveAndFlush(u); }
 String bearer(String t){return "Bearer "+t;}
 long fileCount() throws Exception {try(var s=Files.list(UPLOAD)){return s.count();}}
 MockMultipartFile image(String name,String type) throws Exception {var bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",bytes);return new MockMultipartFile("fotos",name,type,bytes.toByteArray());}
 @Test void anonymousCannotReadPrivateAccount() throws Exception {mvc.perform(get("/v1/usuario/me")).andExpect(status().isUnauthorized());}
 @Test void commonUserCannotListAccounts() throws Exception {mvc.perform(get("/v1/usuario").header("Authorization",bearer(tokenA))).andExpect(status().isForbidden());}
 @Test void commonUserCannotSearchAccounts() throws Exception {mvc.perform(get("/v1/usuario/buscar?nome=B").header("Authorization",bearer(tokenA))).andExpect(status().isForbidden());}
 @Test void publicProfileOmitsPrivateFields() throws Exception {mvc.perform(get("/v1/usuario/"+b.getId()).header("Authorization",bearer(tokenA))).andExpect(status().isOk()).andExpect(jsonPath("$.email").doesNotExist()).andExpect(jsonPath("$.telefone").doesNotExist()).andExpect(jsonPath("$.senha").doesNotExist());}
 @Test void ownAccountKeepsContactsWithoutPassword() throws Exception {mvc.perform(get("/v1/usuario/me").header("Authorization",bearer(tokenA))).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(a.getEmail())).andExpect(jsonPath("$.senha").doesNotExist());}
 @Test void cannotChangeAnotherAccount() throws Exception {mvc.perform(put("/v1/usuario/"+b.getId()).header("Authorization",bearer(tokenA)).contentType("application/json").content("{\"nome\":\"X\",\"email\":\"x@example.com\",\"telefone\":\"38999999999\"}")).andExpect(status().isForbidden());assertEquals("B",usuarios.findById(b.getId()).orElseThrow().getNome());}
 @Test void cannotDeleteAnotherListing() throws Exception {mvc.perform(delete("/v1/anuncio/"+anuncio.getId()).header("Authorization",bearer(tokenA))).andExpect(status().isForbidden());assertTrue(anuncios.existsById(anuncio.getId()));}
 @Test void strangerCannotReadNegotiationOrChat() throws Exception {for(String suffix:new String[]{"","/mensagens"}) mvc.perform(get("/v1/proposta/"+proposta.getId()+suffix).header("Authorization",bearer(tokenC))).andExpect(status().isForbidden());}
 @Test void strangerCannotAcceptOffer() throws Exception {mvc.perform(put("/v1/proposta/"+proposta.getId()+"/aceitar").header("Authorization",bearer(tokenC))).andExpect(status().isForbidden());assertEquals(StatusProposta.PENDENTE,propostas.findById(proposta.getId()).orElseThrow().getStatus());}
 @Test void unauthorizedUploadLeavesNoFiles() throws Exception {mvc.perform(multipart("/v1/anuncio/"+anuncio.getId()+"/fotos").file(image("photo.png","image/png")).header("Authorization",bearer(tokenA))).andExpect(status().isForbidden());assertEquals(0,fileCount());assertEquals(0,fotos.count());}
 @Test void fakeImageIsRejectedWithoutStorage() throws Exception {mvc.perform(multipart("/v1/anuncio/"+anuncio.getId()+"/fotos").file(new MockMultipartFile("fotos","fake.png","image/png","not an image".getBytes())).header("Authorization",bearer(tokenB))).andExpect(status().isBadRequest());assertEquals(0,fileCount());assertEquals(0,fotos.count());}
 @Test void invalidBatchDoesNotPartiallyPersist() throws Exception {mvc.perform(multipart("/v1/anuncio/"+anuncio.getId()+"/fotos").file(image("good.png","image/png")).file(new MockMultipartFile("fotos","bad.png","image/png",new byte[]{1,2,3})).header("Authorization",bearer(tokenB))).andExpect(status().isBadRequest());assertEquals(0,fileCount());assertEquals(0,fotos.count());}
 @Test void executableExtensionIsRejected() throws Exception {mvc.perform(multipart("/v1/anuncio/"+anuncio.getId()+"/fotos").file(image("photo.html","image/png")).header("Authorization",bearer(tokenB))).andExpect(status().isBadRequest());assertEquals(0,fileCount());}
 @Test void malformedTokenReturns401() throws Exception {mvc.perform(get("/v1/usuario/me").header("Authorization","Bearer invalid")).andExpect(status().isUnauthorized());}
 @Test void passwordValidationNeverEchoesRejectedValue() throws Exception {var r=mvc.perform(post("/auth/register").contentType("application/json").content("{\"nome\":\"Test\",\"email\":\"t@example.com\",\"telefone\":\"38999999999\",\"senha\":\"secret\"}")).andExpect(status().isBadRequest()).andReturn();assertFalse(r.getResponse().getContentAsString().contains("secret"));}

 @Test void validImageIsReencodedAndStored() throws Exception {mvc.perform(multipart("/v1/anuncio/"+anuncio.getId()+"/fotos").file(image("photo.png","image/png")).header("Authorization",bearer(tokenB))).andExpect(status().isCreated()).andExpect(jsonPath("$[0].tipoFoto").value("OUTRO"));assertEquals(1,fileCount());assertEquals(1,fotos.count());try(var paths=Files.list(UPLOAD)){assertNotNull(ImageIO.read(paths.findFirst().orElseThrow().toFile()));}}
 @Test void mismatchedMimeIsRejected() throws Exception {mvc.perform(multipart("/v1/anuncio/"+anuncio.getId()+"/fotos").file(image("photo.png","image/jpeg")).header("Authorization",bearer(tokenB))).andExpect(status().isBadRequest());assertEquals(0,fileCount());}
 @Test void oversizedFileIsRejected() throws Exception {mvc.perform(multipart("/v1/anuncio/"+anuncio.getId()+"/fotos").file(new MockMultipartFile("fotos","photo.png","image/png",new byte[5*1024*1024+1])).header("Authorization",bearer(tokenB))).andExpect(status().isBadRequest());assertEquals(0,fileCount());}
 @Test void batchCountIsBounded() throws Exception {var request=multipart("/v1/anuncio/"+anuncio.getId()+"/fotos");for(int i=0;i<11;i++)request.file(image("photo.png","image/png"));mvc.perform(request.header("Authorization",bearer(tokenB))).andExpect(status().isBadRequest());assertEquals(0,fileCount());}
 @Test void dimensionBombIsRejected() throws Exception {var bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(6001,1,BufferedImage.TYPE_INT_RGB),"png",bytes);mvc.perform(multipart("/v1/anuncio/"+anuncio.getId()+"/fotos").file(new MockMultipartFile("fotos","large.png","image/png",bytes.toByteArray())).header("Authorization",bearer(tokenB))).andExpect(status().isBadRequest());assertEquals(0,fileCount());}
 @Test void photoCannotBeDeletedByOtherUser() throws Exception {var f=new AnuncioFoto();f.setAnuncio(anuncio);f.setOrdem(0);f.setUrl("https://example.com/photo.png");f.setTipoFoto(TipoFoto.OUTRO);f=fotos.saveAndFlush(f);mvc.perform(delete("/v1/anuncio/"+anuncio.getId()+"/fotos/"+f.getId()).header("Authorization",bearer(tokenA))).andExpect(status().isForbidden());assertTrue(fotos.existsById(f.getId()));}
 @Test void adminAndReviewerCanReadLegacyAdministration() throws Exception {for(Role role:new Role[]{Role.ADMIN,Role.REVIEWER}){a.setRole(role);usuarios.saveAndFlush(a);mvc.perform(get("/v1/usuario").header("Authorization",bearer(tokenA))).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].senha").doesNotExist());}}
 @Test void adminRouteRequiresAdmin() throws Exception {for(Role role:new Role[]{Role.USER,Role.REVIEWER}){a.setRole(role);usuarios.saveAndFlush(a);mvc.perform(get("/v1/admin/usuarios").header("Authorization",bearer(tokenA))).andExpect(status().isForbidden());}a.setRole(Role.ADMIN);usuarios.saveAndFlush(a);mvc.perform(get("/v1/admin/usuarios").header("Authorization",bearer(tokenA))).andExpect(status().isOk());}
 @Test void blockedStatesInvalidateExistingTokensAndLogin() throws Exception {for(StatusUsuario state:new StatusUsuario[]{StatusUsuario.SUSPENDED,StatusUsuario.BLOCKED,StatusUsuario.DELETED}){a.setStatus(state);usuarios.saveAndFlush(a);mvc.perform(get("/v1/usuario/me").header("Authorization",bearer(tokenA))).andExpect(status().isUnauthorized());mvc.perform(post("/auth/login").contentType("application/json").content("{\"email\":\"a@example.com\",\"senha\":\"senhaTeste123\"}")).andExpect(status().isUnauthorized());}}
 @Test void passwordChangeInvalidatesOldToken() throws Exception {mvc.perform(put("/v1/usuario/"+a.getId()+"/senha").header("Authorization",bearer(tokenA)).contentType("application/json").content("{\"senhaAtual\":\"senhaTeste123\",\"novaSenha\":\"novaSenhaSegura123\"}")).andExpect(status().isOk());mvc.perform(get("/v1/usuario/me").header("Authorization",bearer(tokenA))).andExpect(status().isUnauthorized());}
 @Test void accountDeletionPreservesRowsAndDisablesToken() throws Exception {mvc.perform(delete("/v1/usuario/"+a.getId()).header("Authorization",bearer(tokenA))).andExpect(status().isNoContent());assertEquals(StatusUsuario.DELETED,usuarios.findById(a.getId()).orElseThrow().getStatus());assertTrue(propostas.existsById(proposta.getId()));mvc.perform(get("/v1/usuario/me").header("Authorization",bearer(tokenA))).andExpect(status().isUnauthorized());}
 @Test void registrationCannotEscalateRoleOrState() throws Exception {mvc.perform(post("/auth/register").contentType("application/json").content("{\"nome\":\"D\",\"email\":\"d@example.com\",\"telefone\":\"38999999999\",\"senha\":\"senhaTeste123\",\"role\":\"ADMIN\",\"status\":\"ACTIVE\"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("USER")).andExpect(jsonPath("$.status").value("PENDING_CONTACT_VERIFICATION"));}
 @Test void malformedJsonReturnsSanitizedProblem() throws Exception {var r=mvc.perform(post("/auth/login").contentType("application/json").content("{ secret")).andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith("application/problem+json")).andReturn();assertFalse(r.getResponse().getContentAsString().contains("secret"));}
 @Test void strangerCannotSendChatMessage() throws Exception {mvc.perform(post("/v1/proposta/"+proposta.getId()+"/mensagens").header("Authorization",bearer(tokenC)).contentType("application/json").content("{\"conteudo\":\"Intruso\"}")).andExpect(status().isForbidden());assertEquals(0,mensagens.count());}
 @Test void longChatMessageIsRejected() throws Exception {mvc.perform(post("/v1/proposta/"+proposta.getId()+"/mensagens").header("Authorization",bearer(tokenA)).contentType("application/json").content("{\"conteudo\":\""+"x".repeat(2001)+"\"}")).andExpect(status().isBadRequest());assertEquals(0,mensagens.count());}
}
