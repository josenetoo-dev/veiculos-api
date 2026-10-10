package com.josenetoo_dev.veiculos_api;

import com.josenetoo_dev.veiculos_api.enums.*;
import com.josenetoo_dev.veiculos_api.model.*;
import com.josenetoo_dev.veiculos_api.repository.*;
import com.josenetoo_dev.veiculos_api.security.JwtUtil;
import com.josenetoo_dev.veiculos_api.service.BiometricProvider;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes isolados: MOCK do provedor, nenhuma rede AWS, nenhum custo.
 * Testam as regras reais de vínculo, autorização, submissão e anti-replay.
 */
@SpringBootTest(properties={
  "spring.datasource.url=jdbc:h2:mem:biometria;MODE=MySQL;DB_CLOSE_DELAY=-1",
  "spring.datasource.username=sa", "spring.datasource.password=",
  "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
  "jwt.secret=biometria-test-secret-must-have-32-bytes",
  "verification.biometric.enabled=true",
  "verification.biometric.liveness-threshold=90",
  "verification.biometric.face-similarity-threshold=90",
  "verification.biometric.policy-version=biometria-v1",
  "verification.storage.key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
@AutoConfigureMockMvc(printOnlyOnFailure=false,print=org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint.NONE)
class BiometriaRegressionTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    BiometricProvider provider;

    static final Path PRIVATE;
    static final Path UPLOAD;
    static {
        try {
            PRIVATE=Files.createTempDirectory("auto-minas-biometrics-private-");
            UPLOAD=Files.createTempDirectory("auto-minas-biometrics-public-");
        } catch(Exception e){throw new IllegalStateException(e);}
    }
    @DynamicPropertySource static void dirs(DynamicPropertyRegistry r) {
        r.add("verification.storage.dir",()->PRIVATE.toString());
        r.add("upload.dir",()->UPLOAD.toString());
    }
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository users;
    @Autowired VerificacaoRepository cases;
    @Autowired EvidenciaVerificacaoRepository evidence;
    @Autowired SessaoBiometriaRepository sessions;
    @Autowired EventoVerificacaoRepository events;
    @Autowired JwtUtil jwt;
    @Autowired PasswordEncoder encoder;

    Usuario seller,other,reviewer;
    String sellerToken,otherToken,staffToken;
    Long caseId;

    @BeforeEach void setUp() throws Exception {
        sessions.deleteAll();
        evidence.deleteAll();
        events.deleteAll();
        cases.deleteAll();
        users.deleteAll();
        try(var files=Files.list(PRIVATE)){for(var p:files.toList())Files.delete(p);}
        seller=createUser("Seller","seller-biometria@example.com",Role.USER);
        other=createUser("Other","other-biometria@example.com",Role.USER);
        reviewer=createUser("Reviewer","reviewer-biometria@example.com",Role.REVIEWER);
        sellerToken=jwt.gerarToken(seller.getId().toString());
        otherToken=jwt.gerarToken(other.getId().toString());
        staffToken=jwt.gerarToken(reviewer.getId().toString());
        when(provider.criarSessao()).thenAnswer(x->UUID.randomUUID().toString());
        caseId=Long.parseLong(mvc.perform(post("/v1/verificacoes/identidade")
                .header("Authorization","Bearer "+sellerToken))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString().split("\\"id\\":")[1].split("[,}]")[0].trim());
    }

    Usuario createUser(String name,String email,Role role) {
        Usuario u=new Usuario();
        u.setNome(name);u.setEmail(email);u.setTelefone("38999999999");
        u.setSenha(encoder.encode("senhaDeTeste123"));
        u.setRole(role);u.setStatus(StatusUsuario.ACTIVE);
        return users.saveAndFlush(u);
    }

    MockMultipartFile image(String parameter) throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(10,10,BufferedImage.TYPE_INT_RGB),"png",out);
        return new MockMultipartFile(parameter,"documento.png","image/png",out.toByteArray());
    }

    long addDocument(TipoEvidencia type) throws Exception {
        String json=mvc.perform(multipart("/v1/verificacoes/"+caseId+"/evidencias")
            .file(image("arquivo")).param("tipo",type.name())
            .header("Authorization","Bearer "+sellerToken))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return Long.parseLong(json.split("\\"id\\":")[1].split("[,}]")[0].trim());
    }

    void documents() throws Exception {
        addDocument(TipoEvidencia.IDENTIDADE_FRENTE);
        addDocument(TipoEvidencia.IDENTIDADE_VERSO);
    }

    String start() throws Exception {
        String json=mvc.perform(post("/v1/verificacoes/"+caseId+"/biometria/sessoes")
            .header("Authorization","Bearer "+sellerToken)
            .contentType("application/json")
            .content("{\"aceiteBiometria\":true}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("CRIADA"))
            .andExpect(jsonPath("$.documentoEvidenciaId").doesNotExist())
            .andReturn().getResponse().getContentAsString();
        var matcher=java.util.regex.Pattern.compile("\\\"sessionId\\\"\\s*:\\s*\\\"([a-f0-9-]{36})\\\"").matcher(json);
        assertTrue(matcher.find());
        return matcher.group(1);
    }

    void result(String id,String expected) throws Exception {
        mvc.perform(get("/v1/verificacoes/"+caseId+"/biometria/sessoes/"+id+"/resultado")
            .header("Authorization","Bearer "+sellerToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(expected));
    }

    @Test void needsValidOwnerConsentAndFrontDocumentBeforeAnyAwsSession() throws Exception {
        mvc.perform(post("/v1/verificacoes/"+caseId+"/biometria/sessoes")
             .contentType("application/json").content("{\"aceiteBiometria\":true}"))
             .andExpect(status().isUnauthorized());
        mvc.perform(post("/v1/verificacoes/"+caseId+"/biometria/sessoes")
            .header("Authorization","Bearer "+otherToken)
            .contentType("application/json").content("{\"aceiteBiometria\":true}"))
            .andExpect(status().isNotFound());
        mvc.perform(post("/v1/verificacoes/"+caseId+"/biometria/sessoes")
            .header("Authorization","Bearer "+sellerToken)
            .contentType("application/json").content("{\"aceiteBiometria\":false}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/v1/verificacoes/"+caseId+"/biometria/sessoes")
            .header("Authorization","Bearer "+sellerToken)
            .contentType("application/json").content("{\"aceiteBiometria\":true}"))
            .andExpect(status().isConflict());
        verify(provider,never()).criarSessao();
        assertEquals(0,sessions.count());
    }

    @Test void successfulTechnicalResultDoesNotApproveIdentityAutomatically() throws Exception {
        documents();
        String session=start();
        var data=sessions.findById(session).orElseThrow();
        assertEquals(caseId,data.getVerificacao().getId());
        assertNotNull(data.getAceiteBiometriaEm());
        assertEquals("biometria-v1",data.getVersaoPolitica());
        when(provider.consultar(eq(session),any(byte[].class),eq(90f),eq(90f)))
            .thenReturn(BiometricProvider.Outcome.AGUARDANDO)
            .thenReturn(BiometricProvider.Outcome.APROVADA_TECNICAMENTE);
        result(session,"CRIADA");
        result(session,"APROVADA_TECNICAMENTE");
        result(session,"APROVADA_TECNICAMENTE"); // idempotente, não repete AWS
        verify(provider,times(2)).consultar(eq(session),any(byte[].class),eq(90f),eq(90f));
        assertEquals(StatusVerificacao.RASCUNHO,cases.findById(caseId).orElseThrow().getStatus());
        mvc.perform(post("/v1/verificacoes/"+caseId+"/enviar")
            .header("Authorization","Bearer "+sellerToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_ANALISE"));
        mvc.perform(post("/v1/verificacoes/revisao/"+caseId+"/aprovar")
            .header("Authorization","Bearer "+staffToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADA"));
        assertEquals(1,events.count());
    }

    @Test void inconclusiveFaceBlocksSubmissionAndNeverAutoApproves() throws Exception {
        documents();
        String session=start();
        when(provider.consultar(eq(session),any(byte[].class),anyFloat(),anyFloat()))
            .thenReturn(BiometricProvider.Outcome.INCONCLUSIVA);
        result(session,"INCONCLUSIVA");
        mvc.perform(post("/v1/verificacoes/"+caseId+"/enviar")
            .header("Authorization","Bearer "+sellerToken))
            .andExpect(status().isConflict());
        assertEquals(StatusVerificacao.RASCUNHO,cases.findById(caseId).orElseThrow().getStatus());
    }

    @Test void replacedDocumentRevokesFormerFaceComparisonEvenIfSamePhoto() throws Exception {
        documents();
        String session=start();
        when(provider.consultar(eq(session),any(byte[].class),anyFloat(),anyFloat()))
            .thenReturn(BiometricProvider.Outcome.APROVADA_TECNICAMENTE);
        result(session,"APROVADA_TECNICAMENTE");
        var oldFront=evidence.findByVerificacaoId(caseId).stream()
            .filter(e->e.getTipo()==TipoEvidencia.IDENTIDADE_FRENTE).findFirst().orElseThrow();
        mvc.perform(delete("/v1/verificacoes/"+caseId+"/evidencias/"+oldFront.getId())
            .header("Authorization","Bearer "+sellerToken)).andExpect(status().isNoContent());
        addDocument(TipoEvidencia.IDENTIDADE_FRENTE);
        mvc.perform(post("/v1/verificacoes/"+caseId+"/enviar")
            .header("Authorization","Bearer "+sellerToken)).andExpect(status().isConflict());
        assertEquals(StatusVerificacao.RASCUNHO,cases.findById(caseId).orElseThrow().getStatus());
    }

    @Test void rateLimitAndOtherUserCannotReadOrReuseSession() throws Exception {
        documents();
        String session=start();
        mvc.perform(get("/v1/verificacoes/"+caseId+"/biometria/sessoes/"+session+"/resultado")
            .header("Authorization","Bearer "+otherToken)).andExpect(status().isNotFound());
        mvc.perform(post("/v1/verificacoes/"+caseId+"/biometria/sessoes")
            .header("Authorization","Bearer "+sellerToken).contentType("application/json")
            .content("{\"aceiteBiometria\":true}"))
            .andExpect(status().isTooManyRequests());
        verify(provider,times(1)).criarSessao();
    }

    @Test void awsExpiredSessionBecomesInconclusiveWithoutProviderCall() throws Exception {
        documents();
        String session=start();
        var s=sessions.findById(session).orElseThrow();
        s.setCriadoEm(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(4));
        sessions.saveAndFlush(s);
        result(session,"INCONCLUSIVA");
        verify(provider,never()).consultar(anyString(),any(byte[].class),anyFloat(),anyFloat());
        assertEquals(StatusSessaoBiometria.INCONCLUSIVA,sessions.findById(session).orElseThrow().getStatus());
    }

    @Test void manualReviewStillRequiresFaceResultWhenFeatureEnabled() throws Exception {
        documents();
        mvc.perform(post("/v1/verificacoes/"+caseId+"/enviar")
            .header("Authorization","Bearer "+sellerToken)).andExpect(status().isConflict());
        verify(provider,never()).criarSessao();
        assertEquals(0,events.count());
    }
}
