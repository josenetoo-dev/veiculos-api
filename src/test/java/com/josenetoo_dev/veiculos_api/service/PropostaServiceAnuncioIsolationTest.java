package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.proposta_dto.PropostaRequest;
import com.josenetoo_dev.veiculos_api.enums.StatusAnuncio;
import com.josenetoo_dev.veiculos_api.enums.StatusUsuario;
import com.josenetoo_dev.veiculos_api.model.Anuncio;
import com.josenetoo_dev.veiculos_api.model.Proposta;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.AnuncioRepository;
import com.josenetoo_dev.veiculos_api.repository.PropostaRepository;
import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropostaServiceAnuncioIsolationTest {
    @Mock private PropostaRepository propostas;
    @Mock private AnuncioRepository anuncios;
    @Mock private UsuarioRepository usuarios;
    @Mock private MensagemService mensagens;
    @InjectMocks private PropostaService service;

    private Usuario comprador;
    private Usuario vendedor;
    private Anuncio carroA;
    private Anuncio carroB;

    @BeforeEach
    void setup() {
        comprador = new Usuario();
        comprador.setId(11L);
        comprador.setStatus(StatusUsuario.ACTIVE);
        vendedor = new Usuario();
        vendedor.setId(22L);
        carroA = anuncio(100L);
        carroB = anuncio(200L);
        when(usuarios.findById(11L)).thenReturn(Optional.of(comprador));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("11",null,java.util.List.of()));
    }

    private Anuncio anuncio(Long id) {
        Anuncio ad = new Anuncio();
        ad.setId(id);
        ad.setUsuario(vendedor);
        ad.setStatus(StatusAnuncio.ATIVO);
        return ad;
    }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    @Test
    void sameSellerDifferentListingCreatesIndependentConversation() {
        Proposta earlier = new Proposta();
        earlier.setId(51L);
        earlier.setAnuncio(carroA);
        earlier.setComprador(comprador);

        when(anuncios.findByIdForUpdate(200L)).thenReturn(Optional.of(carroB));
        when(propostas.findFirstByCompradorIdAndAnuncioIdOrderByIdAsc(11L,200L))
                .thenReturn(Optional.empty());
        when(propostas.save(any(Proposta.class))).thenAnswer(call -> {
            Proposta saved=call.getArgument(0);
            saved.setId(52L);
            return saved;
        });

        var created = service.mandarProposta(new PropostaRequest(
                new BigDecimal("90000"),"Interesse no segundo veículo",200L));

        assertEquals(52L,created.getId());
        assertEquals(200L,created.getAnuncio());
        verify(propostas).findFirstByCompradorIdAndAnuncioIdOrderByIdAsc(11L,200L);
        verify(propostas).save(any(Proposta.class));
        verify(mensagens).enviarMensagemDaProposta(any(Proposta.class), eq(comprador), eq("Interesse no segundo veículo"), isNull());
        verify(propostas,never()).entreParticipantes(any(),any(),any());
    }

    @Test
    void sameBuyerSameListingReusesExistingConversation() {
        Proposta existing = new Proposta();
        existing.setId(51L);
        existing.setAnuncio(carroA);
        existing.setComprador(comprador);

        when(anuncios.findByIdForUpdate(100L)).thenReturn(Optional.of(carroA));
        when(propostas.findFirstByCompradorIdAndAnuncioIdOrderByIdAsc(11L,100L))
                .thenReturn(Optional.of(existing));

        var reused=service.mandarProposta(new PropostaRequest(
                new BigDecimal("90000"),"Nova tentativa",100L));

        assertEquals(51L,reused.getId());
        assertEquals(100L,reused.getAnuncio());
        verify(propostas,never()).save(any());
    }

    @Test
    void demoListingSupportsSimulatedNegotiation() {
        carroA.setCodigo("DEMO-001");
        when(anuncios.findByIdForUpdate(100L)).thenReturn(Optional.of(carroA));
        when(propostas.findFirstByCompradorIdAndAnuncioIdOrderByIdAsc(11L,100L))
                .thenReturn(Optional.empty());
        when(propostas.save(any(Proposta.class))).thenAnswer(call -> {
            Proposta saved = call.getArgument(0);
            saved.setId(60L);
            return saved;
        });

        var created = service.mandarProposta(new PropostaRequest(
                new BigDecimal("50000"), "Proposta de demonstração", 100L));

        assertEquals(60L, created.getId());
        verify(mensagens).enviarMensagemDaProposta(any(Proposta.class), eq(comprador),
                eq("Proposta de demonstração"), isNull());
    }
}
