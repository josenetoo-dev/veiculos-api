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
 "logging.level.org.springframework.security=INFO",
 "app.email-change.enabled=true", "app.email-change.from=no-reply@example.com", "spring.mail.host=localhost",
 "app.contact-email.enabled=true", "app.contact-email.from=no-reply@example.com",
 "verification.storage.key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
@org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
@AutoConfigureMockMvc(printOnlyOnFailure = false, print = org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint.NONE)
class SecurityRegressionTest {
 @org.springframework.test.context.bean.override.mockito.MockitoBean
 org.springframework.mail.javamail.JavaMailSender emailSender;

 @Autowired com.josenetoo_dev.veiculos_api.service.AnuncioService anuncioService;

 static final Path UPLOAD;
 static final Path PRIVATE;
 static {
  try {
   UPLOAD = Files.createTempDirectory("auto-minas-security-");
   PRIVATE = Files.createTempDirectory("auto-minas-private-");
  } catch(Exception e) {throw new RuntimeException(e);}
 }
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
  r.add("upload.dir", () -> UPLOAD.toString());
  r.add("verification.storage.dir", () -> PRIVATE.toString());
 }
 @Autowired MockMvc mvc;
 @Autowired org.springframework.context.ApplicationContext context;
 @Autowired UsuarioRepository usuarios;
 @Autowired AnuncioRepository anuncios;
 @Autowired VeiculoRepository veiculos;
 @Autowired AnuncioFotoRepository fotos;
 @Autowired PropostaRepository propostas;
 @Autowired ModeracaoEventoRepository eventos;
 @Autowired VerificacaoRepository verificacoes;
 @Autowired EvidenciaVerificacaoRepository evidencias;
 @Autowired EventoVerificacaoRepository eventosVerificacao;

 @Autowired MensagemRepository mensagens;
 @Autowired JwtUtil jwt;
 @Autowired PasswordEncoder encoder;
 @Autowired org.springframework.transaction.PlatformTransactionManager txManager;
 @Autowired com.josenetoo_dev.veiculos_api.service.AnuncioFotoService fotoService;
 @Autowired com.josenetoo_dev.veiculos_api.service.UsuarioService usuarioService;
 Usuario a, b, c;
 Anuncio anuncio;
 Proposta proposta;
 String tokenA, tokenB, tokenC;
 @BeforeEach void setup() throws Exception {
  mensagens.deleteAll(); propostas.deleteAll(); fotos.deleteAll(); eventos.deleteAll();
  evidencias.deleteAll(); eventosVerificacao.deleteAll(); verificacoes.deleteAll();
  anuncios.deleteAll(); veiculos.deleteAll(); usuarios.deleteAll();
  try(var files=Files.list(UPLOAD)){ for(Path f:files.toList()) Files.delete(f); }
  try(var files=Files.list(PRIVATE)){ for(Path f:files.toList()) Files.delete(f); }
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

 @Test void storageAndRowsAreCleanedWhenFailureFollowsUpload() throws Exception {
  var auth=new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(b.getId().toString(),null,java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
  org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
  var file=image("photo.png","image/png");
  try {assertThrows(IllegalStateException.class,()->new org.springframework.transaction.support.TransactionTemplate(txManager).execute(status->{fotoService.uploadFotos(anuncio.getId(),java.util.List.of(file));throw new IllegalStateException("Forced failure after upload");}));}
  finally {org.springframework.security.core.context.SecurityContextHolder.clearContext();}
  assertEquals(0,fileCount());assertEquals(0,fotos.count());
 }
 @Test void cumulativePhotoLimitIsEnforced() throws Exception {
  for(int i=0;i<20;i++){var f=new AnuncioFoto();f.setAnuncio(anuncio);f.setOrdem(i);f.setUrl("https://example.com/"+i+".png");f.setTipoFoto(TipoFoto.OUTRO);fotos.saveAndFlush(f);}
  mvc.perform(multipart("/v1/anuncio/"+anuncio.getId()+"/fotos").file(image("photo.png","image/png")).header("Authorization",bearer(tokenB))).andExpect(status().isBadRequest());assertEquals(0,fileCount());assertEquals(20,fotos.count());
 }
 @Test void truncatedImageIsRejected() throws Exception {var f=image("photo.png","image/png");mvc.perform(multipart("/v1/anuncio/"+anuncio.getId()+"/fotos").file(new MockMultipartFile("fotos","photo.png","image/png",java.util.Arrays.copyOf(f.getBytes(),20))).header("Authorization",bearer(tokenB))).andExpect(status().isBadRequest());assertEquals(0,fileCount());}
 @Test void pathTraversalFilenameIsRejected() throws Exception {mvc.perform(multipart("/v1/anuncio/"+anuncio.getId()+"/fotos").file(image("../photo.png","image/png")).header("Authorization",bearer(tokenB))).andExpect(status().isBadRequest());assertEquals(0,fileCount());}
 @Test void utf8PasswordCannotExceedBcryptByteLimit() throws Exception {mvc.perform(post("/auth/register").contentType("application/json").content("{\"nome\":\"Test\",\"email\":\"utf8@example.com\",\"telefone\":\"38999999999\",\"senha\":\""+"é".repeat(37)+"\"}")).andExpect(status().isBadRequest());assertFalse(usuarios.existsByEmail("utf8@example.com"));}
 @Test void forgedExpiredAndRevokedTokensAreRejected() throws Exception {
  var key=io.jsonwebtoken.security.Keys.hmacShaKeyFor("integration-test-key-at-least-32-bytes-long".getBytes(java.nio.charset.StandardCharsets.UTF_8));
  String expired=io.jsonwebtoken.Jwts.builder().setSubject(a.getId().toString()).claim("tv",0).setExpiration(new java.util.Date(1)).signWith(key).compact();
  String legacy=io.jsonwebtoken.Jwts.builder().setSubject(a.getId().toString()).setExpiration(new java.util.Date(System.currentTimeMillis()+60000)).signWith(key).compact();
  String badSubject=io.jsonwebtoken.Jwts.builder().setSubject("not-a-number").claim("tv",0).setExpiration(new java.util.Date(System.currentTimeMillis()+60000)).signWith(key).compact();
  String forged=io.jsonwebtoken.Jwts.builder().setSubject(a.getId().toString()).claim("tv",0).setExpiration(new java.util.Date(System.currentTimeMillis()+60000)).signWith(io.jsonwebtoken.security.Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256)).compact();
  for(String t:new String[]{expired,legacy,badSubject,forged})mvc.perform(get("/v1/usuario/me").header("Authorization",bearer(t))).andExpect(status().isUnauthorized());
 }
 @Test void jwtRoleClaimCannotEscalatePrivileges() throws Exception {
  var key=io.jsonwebtoken.security.Keys.hmacShaKeyFor("integration-test-key-at-least-32-bytes-long".getBytes(java.nio.charset.StandardCharsets.UTF_8));
  String t=io.jsonwebtoken.Jwts.builder().setSubject(a.getId().toString()).claim("tv",a.getTokenVersion()).claim("role","ADMIN").setExpiration(new java.util.Date(System.currentTimeMillis()+60000)).signWith(key).compact();
  mvc.perform(get("/v1/admin/usuarios").header("Authorization",bearer(t))).andExpect(status().isForbidden());
 }

 @Test void staleProfileCannotUndoPasswordRevocation() throws Exception {
  runStaleProfileRace(()->{var request=new com.josenetoo_dev.veiculos_api.dto.usuario_dto.TrocarSenhaRequest();request.setSenhaAtual("senhaTeste123");request.setNovaSenha("newSecurePassword123");usuarioService.atualizarSenha(request,a.getId());},false);
  var saved=usuarios.findById(a.getId()).orElseThrow();assertTrue(encoder.matches("newSecurePassword123",saved.getSenha()));assertEquals(1,saved.getTokenVersion());
  mvc.perform(get("/v1/usuario/me").header("Authorization",bearer(tokenA))).andExpect(status().isUnauthorized());
 }
 @Test void staleProfileCannotResurrectDeletedAccount() throws Exception {
  runStaleProfileRace(()->usuarioService.deletarUsuario(a.getId()),true);
  assertEquals(StatusUsuario.DELETED,usuarios.findById(a.getId()).orElseThrow().getStatus());
  mvc.perform(get("/v1/usuario/me").header("Authorization",bearer(tokenA))).andExpect(status().isUnauthorized());
 }
 void runStaleProfileRace(Runnable securityChange,boolean mustReject) throws Exception {
  var ready=new java.util.concurrent.CountDownLatch(1);var resume=new java.util.concurrent.CountDownLatch(1);
  var executor=java.util.concurrent.Executors.newSingleThreadExecutor();
  var future=executor.submit(()->{
   authenticate(a.getId());
   try {new org.springframework.transaction.support.TransactionTemplate(txManager).execute(status->{
    usuarios.findById(a.getId()).orElseThrow();ready.countDown();
    try{if(!resume.await(10,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("Race timeout");}catch(InterruptedException e){throw new RuntimeException(e);}
    var request=new com.josenetoo_dev.veiculos_api.dto.usuario_dto.UsuarioRequest();request.setNome("Updated");request.setEmail(a.getEmail());request.setTelefone(a.getTelefone());
    if(mustReject){assertThrows(com.josenetoo_dev.veiculos_api.exception.ex.CredenciaisInvalidasException.class,()->usuarioService.atualizarUsuario(request,a.getId()));status.setRollbackOnly();}
    else usuarioService.atualizarUsuario(request,a.getId());return null;
   });} finally{org.springframework.security.core.context.SecurityContextHolder.clearContext();}
  });
  try{assertTrue(ready.await(10,java.util.concurrent.TimeUnit.SECONDS));authenticate(a.getId());securityChange.run();resume.countDown();future.get(10,java.util.concurrent.TimeUnit.SECONDS);}
  finally{resume.countDown();executor.shutdownNow();org.springframework.security.core.context.SecurityContextHolder.clearContext();}
 }
 void authenticate(Long id){org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(id.toString(),null,java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))));}

 @Test void aggregatePixelLimitIsEnforced() throws Exception {
  var bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(4500,4500,BufferedImage.TYPE_INT_RGB),"png",bytes);
  mvc.perform(multipart("/v1/anuncio/"+anuncio.getId()+"/fotos").file(new MockMultipartFile("fotos","large.png","image/png",bytes.toByteArray())).header("Authorization",bearer(tokenB))).andExpect(status().isBadRequest());assertEquals(0,fileCount());
 }
 @Test void simultaneousUploadsCannotExceedPhotoLimit() throws Exception {
  for(int i=0;i<19;i++){var f=new AnuncioFoto();f.setAnuncio(anuncio);f.setOrdem(i);f.setUrl("https://example.com/"+i+".png");f.setTipoFoto(TipoFoto.OUTRO);fotos.saveAndFlush(f);}
  var firstWritten=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);var secondStarted=new java.util.concurrent.CountDownLatch(1);
  var executor=java.util.concurrent.Executors.newFixedThreadPool(2);var file=image("photo.png","image/png");
  var first=executor.submit(()->{authenticate(b.getId());try{new org.springframework.transaction.support.TransactionTemplate(txManager).execute(status->{fotoService.uploadFotos(anuncio.getId(),java.util.List.of(file));firstWritten.countDown();try{if(!release.await(10,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("Timeout");}catch(InterruptedException e){throw new RuntimeException(e);}return null;});}finally{org.springframework.security.core.context.SecurityContextHolder.clearContext();}});
  try {
   assertTrue(firstWritten.await(10,java.util.concurrent.TimeUnit.SECONDS));
   var second=executor.submit(()->{authenticate(b.getId());secondStarted.countDown();try{assertThrows(IllegalArgumentException.class,()->fotoService.uploadFotos(anuncio.getId(),java.util.List.of(file)));}finally{org.springframework.security.core.context.SecurityContextHolder.clearContext();}});
   assertTrue(secondStarted.await(10,java.util.concurrent.TimeUnit.SECONDS));
   assertThrows(java.util.concurrent.TimeoutException.class,()->second.get(500,java.util.concurrent.TimeUnit.MILLISECONDS));
   release.countDown();first.get(10,java.util.concurrent.TimeUnit.SECONDS);second.get(10,java.util.concurrent.TimeUnit.SECONDS);
  } finally{release.countDown();executor.shutdownNow();}
  assertEquals(20,fotos.count());assertEquals(1,fileCount());
 }

 @Test void cannotEditAnotherListing() throws Exception {
  String body="""
   {"versao":"LT","documentacao":"Regular","garantia":"Nenhuma","titulo":"Alterado","descricao":"Carro","preco":50000,"marca":"Chevrolet","modelo":"Onix","ano":2022,"quilometragem":100,"cor":"Branco","combustivel":"%s","cambio":"%s","categoria":"%s","segundaMao":false}
   """.formatted(anuncio.getCombustivel().name(),anuncio.getCambio().name(),anuncio.getCategoria().name());
  mvc.perform(put("/v1/anuncio/"+anuncio.getId()).header("Authorization",bearer(tokenA)).contentType("application/json").content(body)).andExpect(status().isForbidden());
  assertEquals("Onix",anuncios.findById(anuncio.getId()).orElseThrow().getTitulo());
 }
 @Test void photoMustBelongToListingInPath() throws Exception {
  var f=new AnuncioFoto();f.setAnuncio(anuncio);f.setOrdem(0);f.setUrl("https://example.com/photo.png");f.setTipoFoto(TipoFoto.OUTRO);f=fotos.saveAndFlush(f);
  mvc.perform(delete("/v1/anuncio/"+(anuncio.getId()+1000)+"/fotos/"+f.getId()).header("Authorization",bearer(tokenB))).andExpect(status().isNotFound());assertTrue(fotos.existsById(f.getId()));
 }

 @Test void logsNeverContainPasswordsOrJwt(org.springframework.boot.test.system.CapturedOutput output) throws Exception {
  assertFalse(context.containsBean("inMemoryUserDetailsManager"));
  var response=mvc.perform(post("/auth/login").contentType("application/json").content("{\"email\":\"a@example.com\",\"senha\":\"senhaTeste123\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  var matcher=java.util.regex.Pattern.compile("\"token\"\\s*:\"([^\"]+)\"").matcher(response);assertTrue(matcher.find());
  assertFalse(output.getAll().contains(matcher.group(1)));assertFalse(output.getAll().contains("senhaTeste123"));assertFalse(output.getAll().contains("Using generated security password"));
  assertFalse(a.toString().contains(a.getSenha()));assertFalse(a.toString().contains(a.getEmail()));
 }

 // Fase 1.1 — visibilidade do marketplace.
 @Test void nonActiveListingsAreHiddenFromPublicSearchAndDetails() throws Exception {
  anuncio.setStatus(StatusAnuncio.PAUSADO);
  anuncio.setCodigo("AM-" + anuncio.getId());
  anuncio.setDestaque(true);
  anuncios.saveAndFlush(anuncio);
  long id = anuncio.getId();
  mvc.perform(get("/v1/anuncio")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
  mvc.perform(get("/v1/anuncio/destaques")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
  mvc.perform(get("/v1/anuncio/categoria/"+anuncio.getCategoria())).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
  mvc.perform(get("/v1/anuncio/"+id)).andExpect(status().isNotFound());
  mvc.perform(get("/v1/anuncio/codigo/"+anuncio.getCodigo())).andExpect(status().isNotFound());
  mvc.perform(get("/v1/anuncio/"+id).header("Authorization",bearer(tokenA))).andExpect(status().isNotFound());
  mvc.perform(get("/v1/anuncio/"+id).header("Authorization",bearer(tokenB))).andExpect(status().isOk());
  mvc.perform(get("/v1/anuncio/status/PAUSADO")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
  mvc.perform(get("/v1/anuncio/status/PAUSADO").header("Authorization",bearer(tokenA))).andExpect(jsonPath("$.totalElements").value(0));
  mvc.perform(get("/v1/anuncio/status/PAUSADO").header("Authorization",bearer(tokenB))).andExpect(jsonPath("$.totalElements").value(1));
  mvc.perform(get("/v1/anuncio/meus")).andExpect(status().isUnauthorized());
  mvc.perform(get("/v1/anuncio/meus").header("Authorization",bearer(tokenA))).andExpect(jsonPath("$.totalElements").value(0));
  mvc.perform(get("/v1/anuncio/meus").header("Authorization",bearer(tokenB))).andExpect(jsonPath("$.totalElements").value(1));
  mvc.perform(get("/v1/anuncio/"+id+"/fotos")).andExpect(status().isNotFound());
 }
 @Test void soldListingIsNotPubliclyVisible() throws Exception {
  anuncio.setStatus(StatusAnuncio.VENDIDO); anuncios.saveAndFlush(anuncio);
  mvc.perform(get("/v1/anuncio")).andExpect(jsonPath("$.totalElements").value(0));
  mvc.perform(get("/v1/anuncio/"+anuncio.getId())).andExpect(status().isNotFound());
  mvc.perform(get("/v1/anuncio/"+anuncio.getId()).header("Authorization",bearer(tokenB))).andExpect(status().isOk());
 }
 @Test void deletingListingRemovesNewImagesFromDiskAfterCommit() throws Exception {
  mvc.perform(multipart("/v1/anuncio/"+anuncio.getId()+"/fotos").file(image("photo.png","image/png"))
    .header("Authorization",bearer(tokenB))).andExpect(status().isCreated());
  assertEquals(1,fileCount());
  mvc.perform(delete("/v1/anuncio/"+anuncio.getId()).header("Authorization",bearer(tokenB)))
    .andExpect(status().isNoContent());
  assertEquals(0,fileCount());
  assertFalse(anuncios.existsById(anuncio.getId()));
 }
 @Test void failedDeletionKeepsImageAfterRollback() throws Exception {
  mvc.perform(multipart("/v1/anuncio/"+anuncio.getId()+"/fotos").file(image("photo.png","image/png"))
    .header("Authorization",bearer(tokenB))).andExpect(status().isCreated());
  authenticate(b.getId());
  try {
   assertThrows(IllegalStateException.class,()->new org.springframework.transaction.support.TransactionTemplate(txManager)
      .execute(status->{anuncioService.deletarAnuncio(anuncio.getId()); throw new IllegalStateException("rollback");}));
  } finally {org.springframework.security.core.context.SecurityContextHolder.clearContext();}
  assertEquals(1,fileCount());
  assertTrue(anuncios.existsById(anuncio.getId()));
 }

 // Fase 1.1 — troca de e-mail com confirmação do endereço novo.
 @Test void directEmailChangeWithoutVerificationIsRejected() throws Exception {
  mvc.perform(put("/v1/usuario/"+a.getId()).header("Authorization",bearer(tokenA))
   .contentType("application/json")
   .content("{\"nome\":\"A\",\"email\":\"changed@example.com\",\"telefone\":\"38999999999\"}"))
   .andExpect(status().isBadRequest());
  assertEquals("a@example.com",usuarios.findById(a.getId()).orElseThrow().getEmail());
 }
 @Test void emailChangeRequiresCorrectCurrentPassword() throws Exception {
  mvc.perform(post("/v1/usuario/me/email-change").header("Authorization",bearer(tokenA))
   .contentType("application/json")
   .content("{\"newEmail\":\"new@example.com\",\"currentPassword\":\"wrong\"}"))
   .andExpect(status().isUnauthorized());
  org.mockito.Mockito.verifyNoInteractions(emailSender);
 }
 @Test void emailChangeNeedsCodeFromNewMailboxAndRevokesOldTokens() throws Exception {
  var body = "{\"newEmail\":\"new@example.com\",\"currentPassword\":\"senhaTeste123\"}";
  mvc.perform(post("/v1/usuario/me/email-change").header("Authorization",bearer(tokenA))
   .contentType("application/json").content(body))
   .andExpect(status().isAccepted()).andExpect(content().string(""));
  var cap = org.mockito.ArgumentCaptor.forClass(org.springframework.mail.SimpleMailMessage.class);
  org.mockito.Mockito.verify(emailSender).send(cap.capture());
  assertArrayEquals(new String[]{"new@example.com"},cap.getValue().getTo());
  var matcher = java.util.regex.Pattern.compile("Código: ([A-Za-z0-9_-]+)").matcher(cap.getValue().getText());
  assertTrue(matcher.find());
  String code = matcher.group(1);
  var pending = usuarios.findById(a.getId()).orElseThrow();
  assertEquals("a@example.com",pending.getEmail());
  assertEquals("new@example.com",pending.getPendingEmail());
  assertNotEquals(code,pending.getPendingEmailTokenHash());
  mvc.perform(post("/v1/usuario/me/email-change/confirm").header("Authorization",bearer(tokenA))
   .contentType("application/json").content("{\"token\":\"invalid-but-long-enough-token-string\"}"))
   .andExpect(status().isBadRequest());
  mvc.perform(post("/v1/usuario/me/email-change/confirm").header("Authorization",bearer(tokenA))
   .contentType("application/json").content("{\"token\":\""+code+"\"}"))
   .andExpect(status().isNoContent());
  var changed = usuarios.findById(a.getId()).orElseThrow();
  assertEquals("new@example.com",changed.getEmail());
  assertNull(changed.getPendingEmailTokenHash());
  assertEquals(1L,changed.getTokenVersion());
  mvc.perform(get("/v1/usuario/me").header("Authorization",bearer(tokenA)))
   .andExpect(status().isUnauthorized());
  mvc.perform(post("/auth/login").contentType("application/json")
   .content("{\"email\":\"a@example.com\",\"senha\":\"senhaTeste123\"}"))
   .andExpect(status().isUnauthorized());
  var login = mvc.perform(post("/auth/login").contentType("application/json")
   .content("{\"email\":\"new@example.com\",\"senha\":\"senhaTeste123\"}"))
   .andExpect(status().isOk()).andReturn();
  var loginTokenMatcher = java.util.regex.Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"")
    .matcher(login.getResponse().getContentAsString());
  assertTrue(loginTokenMatcher.find());
  mvc.perform(post("/v1/usuario/me/email-change/confirm")
    .header("Authorization",bearer(loginTokenMatcher.group(1)))
    .contentType("application/json").content("{\"token\":\""+code+"\"}"))
    .andExpect(status().isBadRequest());
 }
 @Test void emailChangeCodeExpiresAndCannotBeUsed() throws Exception {
  mvc.perform(post("/v1/usuario/me/email-change").header("Authorization",bearer(tokenA))
   .contentType("application/json")
   .content("{\"newEmail\":\"new@example.com\",\"currentPassword\":\"senhaTeste123\"}"))
   .andExpect(status().isAccepted());
  var cap=org.mockito.ArgumentCaptor.forClass(org.springframework.mail.SimpleMailMessage.class);
  org.mockito.Mockito.verify(emailSender).send(cap.capture());
  var m=java.util.regex.Pattern.compile("Código: ([A-Za-z0-9_-]+)").matcher(cap.getValue().getText());
  assertTrue(m.find());
  var user=usuarios.findById(a.getId()).orElseThrow();
  user.setPendingEmailExpiresAt(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(1));
  usuarios.saveAndFlush(user);
  mvc.perform(post("/v1/usuario/me/email-change/confirm").header("Authorization",bearer(tokenA))
   .contentType("application/json").content("{\"token\":\""+m.group(1)+"\"}"))
   .andExpect(status().isBadRequest());
  assertEquals("a@example.com",usuarios.findById(a.getId()).orElseThrow().getEmail());
 }
 @Test void emailChangeRequestIsRateLimited() throws Exception {
  String body="{\"newEmail\":\"new@example.com\",\"currentPassword\":\"senhaTeste123\"}";
  mvc.perform(post("/v1/usuario/me/email-change").header("Authorization",bearer(tokenA))
   .contentType("application/json").content(body)).andExpect(status().isAccepted());
  mvc.perform(post("/v1/usuario/me/email-change").header("Authorization",bearer(tokenA))
   .contentType("application/json").content(body)).andExpect(status().isTooManyRequests());
  org.mockito.Mockito.verify(emailSender,org.mockito.Mockito.times(1))
    .send(org.mockito.ArgumentMatchers.any(org.springframework.mail.SimpleMailMessage.class));
 }


 // Fase 2A — publicacao sujeita à moderação administrativa.
 private String listingJson(String title) {
  return """
     {"versao":"LT","documentacao":"Regular","garantia":"Nenhuma","titulo":"%s","descricao":"Descricao do carro",
      "preco":50000,"marca":"Chevrolet","modelo":"Onix","ano":2022,"quilometragem":100,
      "cor":"Branco","combustivel":"%s","cambio":"%s","categoria":"%s","segundaMao":false}
      """.formatted(title, anuncio.getCombustivel().name(), anuncio.getCambio().name(), anuncio.getCategoria().name());
 }
 private void makeReviewerActiveSellerAndPendingListing() {
  a.setRole(Role.REVIEWER);
  a.setStatus(StatusUsuario.ACTIVE);
  usuarios.saveAndFlush(a);
  b.setStatus(StatusUsuario.ACTIVE);
  usuarios.saveAndFlush(b);
  anuncio.setStatus(StatusAnuncio.PENDENTE);
  anuncio.setQuilometragem(100); // mesmo valor usado nos DTOs dos testes de edição
  anuncio.sincronizarVeiculo();
  anuncios.saveAndFlush(anuncio);
  // Fixture representa decisões já auditadas em testes antigos de moderação.
  var iv=Verificacao.identidade(b);
  iv.setStatus(StatusVerificacao.APROVADA);
  verificacoes.saveAndFlush(iv);
  var vv=Verificacao.veiculo(b, anuncio.getVeiculo());
  vv.setStatus(StatusVerificacao.APROVADA);
  verificacoes.saveAndFlush(vv);
 }
 @Test void newListingIsPendingAndCannotBypassReview() throws Exception {
  mvc.perform(post("/v1/anuncio").header("Authorization",bearer(tokenB))
       .contentType("application/json").content(listingJson("Novo carro")))
       .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDENTE"));
  assertEquals(1, anuncios.findByStatus(StatusAnuncio.PENDENTE,
         org.springframework.data.domain.Pageable.unpaged()).getTotalElements());
  mvc.perform(get("/v1/anuncio")).andExpect(jsonPath("$.totalElements").value(1));
  mvc.perform(get("/v1/anuncio/status/PENDENTE")).andExpect(jsonPath("$.totalElements").value(0));
  mvc.perform(get("/v1/anuncio/status/PENDENTE").header("Authorization",bearer(tokenB)))
       .andExpect(jsonPath("$.totalElements").value(1));
  assertEquals(0, eventos.count());
 }
 @Test void reviewerCanApproveAndPublishesWithAuditAndNoDuplicateApproval() throws Exception {
  makeReviewerActiveSellerAndPendingListing();
  mvc.perform(get("/v1/moderacao/anuncios")).andExpect(status().isUnauthorized());
  mvc.perform(get("/v1/moderacao/anuncios").header("Authorization",bearer(tokenC)))
       .andExpect(status().isForbidden());
  mvc.perform(get("/v1/moderacao/anuncios").header("Authorization",bearer(tokenA)))
       .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
  mvc.perform(post("/v1/moderacao/anuncios/"+anuncio.getId()+"/aprovar")
       .header("Authorization",bearer(tokenA)))
       .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ATIVO"));
  var persisted=anuncios.findById(anuncio.getId()).orElseThrow();
  assertEquals(StatusAnuncio.ATIVO,persisted.getStatus());
  assertEquals(a.getId(),persisted.getRevisadoPorId());
  assertNotNull(persisted.getRevisadoEm());
  assertEquals(1,eventos.count());
  var audit=eventos.findAll().get(0);
  assertEquals(StatusAnuncio.PENDENTE,audit.getStatusAnterior());
  assertEquals(StatusAnuncio.ATIVO,audit.getStatusNovo());
  mvc.perform(get("/v1/anuncio/"+anuncio.getId())).andExpect(status().isOk());
  mvc.perform(post("/v1/moderacao/anuncios/"+anuncio.getId()+"/aprovar")
       .header("Authorization",bearer(tokenA))).andExpect(status().isConflict());
  assertEquals(1,eventos.count());
 }
 @Test void pendingSellerCannotReceivePublicationApproval() throws Exception {
  a.setRole(Role.REVIEWER); a.setStatus(StatusUsuario.ACTIVE); usuarios.saveAndFlush(a);
  anuncio.setStatus(StatusAnuncio.PENDENTE); anuncios.saveAndFlush(anuncio);
  mvc.perform(post("/v1/moderacao/anuncios/"+anuncio.getId()+"/aprovar")
       .header("Authorization",bearer(tokenA))).andExpect(status().isConflict());
  assertEquals(StatusAnuncio.PENDENTE,anuncios.findById(anuncio.getId()).orElseThrow().getStatus());
  assertEquals(0,eventos.count());
 }
 @Test void reviewerCannotReviewOwnListing() throws Exception {
  a.setRole(Role.REVIEWER); usuarios.saveAndFlush(a);
  anuncio.setUsuario(a); anuncio.setStatus(StatusAnuncio.PENDENTE);
  anuncios.saveAndFlush(anuncio);
  mvc.perform(post("/v1/moderacao/anuncios/"+anuncio.getId()+"/aprovar")
       .header("Authorization",bearer(tokenA))).andExpect(status().isForbidden());
  assertEquals(0,eventos.count());
 }
 @Test void rejectionIsPrivateAndSellerCanCorrectAndResubmit() throws Exception {
  makeReviewerActiveSellerAndPendingListing();
  mvc.perform(post("/v1/moderacao/anuncios/"+anuncio.getId()+"/rejeitar")
       .header("Authorization",bearer(tokenA)).contentType("application/json")
       .content("{\"motivo\":\"Falta foto do painel\"}"))
       .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJEITADO"))
       .andExpect(jsonPath("$.motivoRejeicao").value("Falta foto do painel"));
  mvc.perform(get("/v1/anuncio/"+anuncio.getId())).andExpect(status().isNotFound());
  mvc.perform(get("/v1/anuncio/meus").header("Authorization",bearer(tokenB)))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$.content[0].motivoRejeicao").value("Falta foto do painel"));
  mvc.perform(get("/v1/anuncio/"+anuncio.getId()).header("Authorization",bearer(tokenC)))
       .andExpect(status().isNotFound());
  assertEquals(StatusAnuncio.REJEITADO,eventos.findAll().get(0).getStatusNovo());
  mvc.perform(put("/v1/anuncio/"+anuncio.getId()).header("Authorization",bearer(tokenB))
       .contentType("application/json").content(listingJson("Carro corrigido")))
       .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDENTE"))
       .andExpect(jsonPath("$.motivoRejeicao").doesNotExist());
  assertNull(anuncios.findById(anuncio.getId()).orElseThrow().getRevisadoPorId());
  assertNull(anuncios.findById(anuncio.getId()).orElseThrow().getMotivoRejeicao());
  mvc.perform(post("/v1/moderacao/anuncios/"+anuncio.getId()+"/aprovar")
       .header("Authorization",bearer(tokenA))).andExpect(status().isOk());
  assertEquals(2,eventos.count());
 }
 @Test void editingApprovedListingRequiresNewModeration() throws Exception {
  mvc.perform(put("/v1/anuncio/"+anuncio.getId()).header("Authorization",bearer(tokenB))
       .contentType("application/json").content(listingJson("Carro alterado")))
       .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDENTE"));
  mvc.perform(get("/v1/anuncio/"+anuncio.getId())).andExpect(status().isNotFound());
 }
 @Test void normalUserCannotModerateAndBlankReasonIsRejected() throws Exception {
  makeReviewerActiveSellerAndPendingListing();
  mvc.perform(post("/v1/moderacao/anuncios/"+anuncio.getId()+"/aprovar")
       .header("Authorization",bearer(tokenC))).andExpect(status().isForbidden());
  mvc.perform(post("/v1/moderacao/anuncios/"+anuncio.getId()+"/rejeitar")
       .header("Authorization",bearer(tokenA)).contentType("application/json")
       .content("{\"motivo\":\"   \"}")).andExpect(status().isBadRequest());
  assertEquals(0,eventos.count());
  assertEquals(StatusAnuncio.PENDENTE,anuncios.findById(anuncio.getId()).orElseThrow().getStatus());
 }


 @Test void unverifiedReviewerCannotReadQueueOrApprove() throws Exception {
  a.setRole(Role.REVIEWER); usuarios.saveAndFlush(a); // PENDING_CONTACT_VERIFICATION
  b.setStatus(StatusUsuario.ACTIVE); usuarios.saveAndFlush(b);
  anuncio.setStatus(StatusAnuncio.PENDENTE); anuncios.saveAndFlush(anuncio);
  mvc.perform(get("/v1/moderacao/anuncios").header("Authorization",bearer(tokenA)))
       .andExpect(status().isForbidden());
  mvc.perform(post("/v1/moderacao/anuncios/"+anuncio.getId()+"/aprovar")
       .header("Authorization",bearer(tokenA))).andExpect(status().isForbidden());
  assertEquals(0,eventos.count());
 }


 // Fase 2B — persistência física distinta para veículos e anúncios.
 @Test void listingCreationPersistsVehicleSeparatelyAndPreservesApiFields() throws Exception {
  mvc.perform(post("/v1/anuncio").header("Authorization",bearer(tokenB))
       .contentType("application/json").content(listingJson("Novo cadastro")))
       .andExpect(status().isCreated())
       .andExpect(jsonPath("$.marca").value("Chevrolet"))
       .andExpect(jsonPath("$.modelo").value("Onix"))
       .andExpect(jsonPath("$.status").value("PENDENTE"))
       .andExpect(jsonPath("$.veiculoId").isNumber());
  var novo=anuncios.findAll().stream().filter(a1 -> "Novo cadastro".equals(a1.getTitulo()))
       .findFirst().orElseThrow();
  assertNotNull(novo.getVeiculo());
  var veiculo=veiculos.findById(novo.getVeiculo().getId()).orElseThrow();
  assertEquals(novo.getMarca(),veiculo.getMarca());
  assertEquals(novo.getModelo(),veiculo.getModelo());
  assertEquals(novo.getVersao(),veiculo.getVersao());
  assertEquals(novo.getAno(),veiculo.getAno());
  assertEquals(novo.getCombustivel(),veiculo.getCombustivel());
  assertEquals(novo.getCambio(),veiculo.getCambio());
  assertEquals(b.getId(),veiculo.getCadastradoPor().getId());
  assertNotEquals(novo.getId(),0L);
 }
 @Test void editingListingKeepsVehicleAndLegacySnapshotInSync() throws Exception {
  long vehicleId=anuncio.getVeiculo().getId();
  String updated=listingJson("Atualizado").replace("\"marca\":\"Chevrolet\"",
           "\"marca\":\"Fiat\"").replace("\"modelo\":\"Onix\"",
           "\"modelo\":\"Argo\"");
  mvc.perform(put("/v1/anuncio/"+anuncio.getId()).header("Authorization",bearer(tokenB))
       .contentType("application/json").content(updated))
       .andExpect(status().isOk()).andExpect(jsonPath("$.veiculoId").value(vehicleId))
       .andExpect(jsonPath("$.marca").value("Fiat"))
       .andExpect(jsonPath("$.status").value("PENDENTE"));
  var after=anuncios.findById(anuncio.getId()).orElseThrow();
  var veh=veiculos.findById(vehicleId).orElseThrow();
  assertEquals("Fiat",after.getMarca());
  assertEquals("Fiat",veh.getMarca());
  assertEquals("Argo",after.getModelo());
  assertEquals("Argo",veh.getModelo());
  assertEquals(vehicleId,veh.getId());
 }
 @Test void photosAndProposalsStillRelateToAnnouncementNotVehicle() throws Exception {
  assertEquals(anuncio.getId(), proposta.getAnuncio().getId());
  assertNotNull(anuncio.getVeiculo().getId());
  mvc.perform(get("/v1/anuncio/"+anuncio.getId()))
       .andExpect(status().isOk()).andExpect(jsonPath("$.veiculoId").isNumber());
 }


 // Fase 3 — verificações documentais humanas sem consultas governamentais.
 long idFromJson(org.springframework.test.web.servlet.MvcResult result) throws Exception {
  var match=java.util.regex.Pattern.compile("\\\"id\\\"\\s*:\\s*(\\d+)")
       .matcher(result.getResponse().getContentAsString());
  assertTrue(match.find()); return Long.parseLong(match.group(1));
 }
 MockMultipartFile privatePhoto() throws Exception {
  var bytes=image("photo.png","image/png").getBytes();
  return new MockMultipartFile("arquivo","documento.png","image/png",bytes);
 }
 long beginIdentity(String jwt) throws Exception {
  return idFromJson(mvc.perform(post("/v1/verificacoes/identidade")
      .header("Authorization",bearer(jwt))).andExpect(status().isCreated()).andReturn());
 }
 long uploadDoc(String jwt,long verificationId,TipoEvidencia kind) throws Exception {
  return idFromJson(mvc.perform(multipart("/v1/verificacoes/"+verificationId+"/evidencias")
      .file(privatePhoto()).param("tipo",kind.name()).header("Authorization",bearer(jwt)))
      .andExpect(status().isCreated()).andReturn());
 }
 void approveIdentityViaApi() throws Exception {
  a.setRole(Role.REVIEWER);a.setStatus(StatusUsuario.ACTIVE);usuarios.saveAndFlush(a);
  b.setStatus(StatusUsuario.ACTIVE);usuarios.saveAndFlush(b);
  long id=beginIdentity(tokenB);
  uploadDoc(tokenB,id,TipoEvidencia.IDENTIDADE_FRENTE);
  uploadDoc(tokenB,id,TipoEvidencia.IDENTIDADE_VERSO);
  mvc.perform(post("/v1/verificacoes/"+id+"/enviar").header("Authorization",bearer(tokenB)))
      .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_ANALISE"));
  mvc.perform(post("/v1/verificacoes/revisao/"+id+"/aprovar")
      .header("Authorization",bearer(tokenA))).andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("APROVADA"));
 }
 @Test void identityDocsAreEncryptedPrivateAndOwnerScoped() throws Exception {
  long verificationId=beginIdentity(tokenB);
  mvc.perform(post("/v1/verificacoes/identidade").header("Authorization",bearer(tokenB)))
       .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(verificationId));
  mvc.perform(get("/v1/verificacoes/"+verificationId)
       .header("Authorization",bearer(tokenC))).andExpect(status().isForbidden());
  long evidenceId=uploadDoc(tokenB,verificationId,TipoEvidencia.IDENTIDADE_FRENTE);
  mvc.perform(get("/v1/verificacoes/"+verificationId+"/evidencias")
       .header("Authorization",bearer(tokenB))).andExpect(status().isOk())
       .andExpect(jsonPath("$[0].id").value(evidenceId))
       .andExpect(jsonPath("$[0].arquivoChave").doesNotExist())
       .andExpect(jsonPath("$[0].tipo").value("IDENTIDADE_FRENTE"));
  var out=mvc.perform(get("/v1/verificacoes/"+verificationId+"/evidencias/"+evidenceId+"/arquivo")
       .header("Authorization",bearer(tokenB)))
       .andExpect(status().isOk())
       .andExpect(header().string("Cache-Control","no-store, private"))
       .andExpect(header().string("X-Content-Type-Options","nosniff")).andReturn();
  assertTrue(out.getResponse().getContentAsByteArray().length>30);
  mvc.perform(get("/v1/verificacoes/"+verificationId+"/evidencias/"+evidenceId+"/arquivo")
       .header("Authorization",bearer(tokenC))).andExpect(status().isForbidden());
  mvc.perform(get("/v1/verificacoes/"+verificationId+"/evidencias/"+evidenceId+"/arquivo"))
       .andExpect(status().isUnauthorized());
  mvc.perform(get("/uploads/fotos/"+evidenceId+".enc")).andExpect(status().isNotFound());
  try(var files=Files.list(PRIVATE)) {
   var paths=files.toList();assertEquals(1,paths.size());
   assertFalse(java.util.Arrays.equals(out.getResponse().getContentAsByteArray(), Files.readAllBytes(paths.get(0))));
   assertEquals(".enc",paths.get(0).getFileName().toString().substring(paths.get(0).getFileName().toString().length()-4));
  }
 }
 @Test void missingEvidenceBlocksSubmissionAndNonOwnerCannotUploadOrDelete() throws Exception {
  long id=beginIdentity(tokenB);
  mvc.perform(post("/v1/verificacoes/"+id+"/enviar").header("Authorization",bearer(tokenB)))
       .andExpect(status().isBadRequest());
  mvc.perform(multipart("/v1/verificacoes/"+id+"/evidencias").file(privatePhoto())
      .param("tipo","IDENTIDADE_FRENTE").header("Authorization",bearer(tokenC)))
      .andExpect(status().isForbidden());
  long evidence=uploadDoc(tokenB,id,TipoEvidencia.IDENTIDADE_FRENTE);
  mvc.perform(post("/v1/verificacoes/"+id+"/enviar").header("Authorization",bearer(tokenB)))
       .andExpect(status().isBadRequest());
  mvc.perform(delete("/v1/verificacoes/"+id+"/evidencias/"+evidence)
       .header("Authorization",bearer(tokenC))).andExpect(status().isForbidden());
  mvc.perform(delete("/v1/verificacoes/"+id+"/evidencias/"+evidence)
       .header("Authorization",bearer(tokenB))).andExpect(status().isNoContent());
  assertEquals(0,evidencias.count());
  try(var files=Files.list(PRIVATE)){assertEquals(0,files.count());}
 }
 @Test void reviewerCannotApproveOwnIdentityOrUnsubmittedCase() throws Exception {
  a.setRole(Role.REVIEWER);a.setStatus(StatusUsuario.ACTIVE);usuarios.saveAndFlush(a);
  long ownerId=beginIdentity(tokenA);
  mvc.perform(post("/v1/verificacoes/revisao/"+ownerId+"/aprovar")
       .header("Authorization",bearer(tokenA))).andExpect(status().isConflict());
  uploadDoc(tokenA,ownerId,TipoEvidencia.IDENTIDADE_FRENTE);
  uploadDoc(tokenA,ownerId,TipoEvidencia.IDENTIDADE_VERSO);
  mvc.perform(post("/v1/verificacoes/"+ownerId+"/enviar").header("Authorization",bearer(tokenA)))
       .andExpect(status().isOk());
  mvc.perform(post("/v1/verificacoes/revisao/"+ownerId+"/aprovar")
       .header("Authorization",bearer(tokenA))).andExpect(status().isForbidden());
  mvc.perform(get("/v1/verificacoes/revisao/pendentes")
       .header("Authorization",bearer(tokenC))).andExpect(status().isForbidden());
  assertEquals(0,eventosVerificacao.count());
 }
 @Test void sellerCannotPublishWithoutIdentityAndVehicleApproval() throws Exception {
  a.setRole(Role.REVIEWER);a.setStatus(StatusUsuario.ACTIVE);usuarios.saveAndFlush(a);
  b.setStatus(StatusUsuario.ACTIVE);usuarios.saveAndFlush(b);
  anuncio.setStatus(StatusAnuncio.PENDENTE);anuncios.saveAndFlush(anuncio);
  mvc.perform(post("/v1/moderacao/anuncios/"+anuncio.getId()+"/aprovar")
       .header("Authorization",bearer(tokenA))).andExpect(status().isConflict());
  assertEquals(StatusAnuncio.PENDENTE,anuncios.findById(anuncio.getId()).orElseThrow().getStatus());
 }
 @Test void fullManualReviewWorkflowUnlocksAdApproval() throws Exception {
  approveIdentityViaApi();
  long vehicleId=anuncio.getVeiculo().getId();
  long id=idFromJson(mvc.perform(post("/v1/verificacoes/veiculos/"+vehicleId)
        .header("Authorization",bearer(tokenB))).andExpect(status().isCreated()).andReturn());
  mvc.perform(multipart("/v1/verificacoes/"+id+"/evidencias").file(privatePhoto())
      .param("tipo","IDENTIDADE_FRENTE").header("Authorization",bearer(tokenB)))
      .andExpect(status().isBadRequest());
  uploadDoc(tokenB,id,TipoEvidencia.DOCUMENTO_VEICULO);
  mvc.perform(post("/v1/verificacoes/"+id+"/enviar").header("Authorization",bearer(tokenB)))
       .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_ANALISE"));
  mvc.perform(post("/v1/verificacoes/revisao/"+id+"/aprovar")
       .header("Authorization",bearer(tokenA)))
       .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADA"));
  anuncio.setStatus(StatusAnuncio.PENDENTE);anuncios.saveAndFlush(anuncio);
  mvc.perform(post("/v1/moderacao/anuncios/"+anuncio.getId()+"/aprovar")
       .header("Authorization",bearer(tokenA)))
       .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ATIVO"));
  assertEquals(2,eventosVerificacao.count());
 }
 @Test void editingVehicleInvalidatesPriorApprovalAndDocuments() throws Exception {
  approveIdentityViaApi();
  long verificationId=idFromJson(mvc.perform(post("/v1/verificacoes/veiculos/"+anuncio.getVeiculo().getId())
       .header("Authorization",bearer(tokenB))).andExpect(status().isCreated()).andReturn());
  uploadDoc(tokenB,verificationId,TipoEvidencia.DOCUMENTO_VEICULO);
  mvc.perform(post("/v1/verificacoes/"+verificationId+"/enviar").header("Authorization",bearer(tokenB)))
      .andExpect(status().isOk());
  mvc.perform(post("/v1/verificacoes/revisao/"+verificationId+"/aprovar")
      .header("Authorization",bearer(tokenA))).andExpect(status().isOk());
  mvc.perform(put("/v1/anuncio/"+anuncio.getId()).header("Authorization",bearer(tokenB))
      .contentType("application/json")
      .content(listingJson("Editado").replace("\"marca\":\"Chevrolet\"",
                        "\"marca\":\"Fiat\"")))
      .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDENTE"));
  assertEquals(StatusVerificacao.RASCUNHO, verificacoes.findById(verificationId).orElseThrow().getStatus());
  assertEquals(2,evidencias.count());
  // Permanecem as duas evidências da identidade, mas a evidência antiga do veículo foi apagada.
  try(var files=Files.list(PRIVATE)){assertEquals(2,files.count());}
  mvc.perform(post("/v1/moderacao/anuncios/"+anuncio.getId()+"/aprovar")
       .header("Authorization",bearer(tokenA))).andExpect(status().isConflict());
 }
 @Test void privateStorageFailsClosedWithoutKey() throws Exception {
  var disabled=new com.josenetoo_dev.veiculos_api.service.PrivateEvidenceStorage(
       PRIVATE.resolve("disabled").toString(),"",UPLOAD.toString());
  assertFalse(disabled.available());
  assertThrows(com.josenetoo_dev.veiculos_api.exception.ex.ArmazenamentoPrivadoIndisponivelException.class,
       ()->disabled.read(java.util.UUID.randomUUID().toString()));
 }


 // Fase 3B — confirmação de e-mail antes de ativar a conta.
 @Test void contactEmailFlowSendsCodeOnlyToMailboxAndActivatesAccount() throws Exception {
  mvc.perform(post("/v1/usuario/me/contato/email/solicitar"))
       .andExpect(status().isUnauthorized());
  mvc.perform(post("/v1/usuario/me/contato/email/solicitar")
       .header("Authorization",bearer(tokenA)))
       .andExpect(status().isAccepted()).andExpect(content().string(""));
  var captor=org.mockito.ArgumentCaptor.forClass(org.springframework.mail.SimpleMailMessage.class);
  org.mockito.Mockito.verify(emailSender).send(captor.capture());
  assertArrayEquals(new String[]{"a@example.com"},captor.getValue().getTo());
  var matcher=java.util.regex.Pattern.compile("(?m)^([A-Za-z0-9_-]{40,})$").matcher(captor.getValue().getText());
  assertTrue(matcher.find());
  String code=matcher.group(1);
  var before=usuarios.findById(a.getId()).orElseThrow();
  assertEquals(StatusUsuario.PENDING_CONTACT_VERIFICATION,before.getStatus());
  assertNotEquals(code,before.getContactEmailTokenHash());
  assertNotNull(before.getContactEmailExpiresAt());
  mvc.perform(post("/v1/usuario/me/contato/email/confirmar")
      .header("Authorization",bearer(tokenA)).contentType("application/json")
      .content("{\"token\":\"not-the-correct-but-long-enough-code\"}"))
      .andExpect(status().isBadRequest());
  mvc.perform(post("/v1/usuario/me/contato/email/confirmar")
      .header("Authorization",bearer(tokenA)).contentType("application/json")
      .content("{\"token\":\""+code+"\"}")).andExpect(status().isNoContent());
  var after=usuarios.findById(a.getId()).orElseThrow();
  assertEquals(StatusUsuario.ACTIVE,after.getStatus());
  assertNotNull(after.getContactEmailVerifiedAt());
  assertNull(after.getContactEmailTokenHash());
  assertEquals(1L,after.getTokenVersion());
  mvc.perform(get("/v1/usuario/me").header("Authorization",bearer(tokenA)))
      .andExpect(status().isUnauthorized());
  var login=mvc.perform(post("/auth/login").contentType("application/json")
      .content("{\"email\":\"a@example.com\",\"senha\":\"senhaTeste123\"}"))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  var newTokenMatcher= java.util.regex.Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(login);
  assertTrue(newTokenMatcher.find());
  mvc.perform(post("/v1/usuario/me/contato/email/confirmar")
      .header("Authorization",bearer(newTokenMatcher.group(1))).contentType("application/json")
      .content("{\"token\":\""+code+"\"}")).andExpect(status().isBadRequest());
 }
 @Test void contactEmailRequestIsThrottledPerAccount() throws Exception {
  mvc.perform(post("/v1/usuario/me/contato/email/solicitar")
      .header("Authorization",bearer(tokenA))).andExpect(status().isAccepted());
  mvc.perform(post("/v1/usuario/me/contato/email/solicitar")
      .header("Authorization",bearer(tokenA))).andExpect(status().isTooManyRequests());
  org.mockito.Mockito.verify(emailSender,org.mockito.Mockito.times(1))
      .send(org.mockito.ArgumentMatchers.any(org.springframework.mail.SimpleMailMessage.class));
 }
 @Test void expiredContactTokenDoesNotActivateAccount() throws Exception {
  mvc.perform(post("/v1/usuario/me/contato/email/solicitar")
      .header("Authorization",bearer(tokenA))).andExpect(status().isAccepted());
  var mailCaptor=org.mockito.ArgumentCaptor.forClass(org.springframework.mail.SimpleMailMessage.class);
  org.mockito.Mockito.verify(emailSender).send(mailCaptor.capture());
  var matcher=java.util.regex.Pattern.compile("(?m)^([A-Za-z0-9_-]{40,})$").matcher(mailCaptor.getValue().getText());
  assertTrue(matcher.find());
  a=usuarios.findById(a.getId()).orElseThrow();
  a.setContactEmailExpiresAt(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).minusSeconds(1));
  usuarios.saveAndFlush(a);
  mvc.perform(post("/v1/usuario/me/contato/email/confirmar")
      .header("Authorization",bearer(tokenA)).contentType("application/json")
      .content("{\"token\":\""+matcher.group(1)+"\"}")).andExpect(status().isBadRequest());
  assertEquals(StatusUsuario.PENDING_CONTACT_VERIFICATION,
       usuarios.findById(a.getId()).orElseThrow().getStatus());
 }

}
