package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.denuncia.DenunciaRequest;
import com.josenetoo_dev.veiculos_api.enums.CategoriaDenuncia;
import com.josenetoo_dev.veiculos_api.enums.StatusAnuncio;
import com.josenetoo_dev.veiculos_api.enums.StatusUsuario;
import com.josenetoo_dev.veiculos_api.exception.ex.AcessoNegadoException;
import com.josenetoo_dev.veiculos_api.model.Anuncio;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.AnuncioRepository;
import com.josenetoo_dev.veiculos_api.repository.DenunciaRepository;
import com.josenetoo_dev.veiculos_api.repository.ModeracaoEventoRepository;
import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DenunciaProprioAnuncioTest {
    @Mock DenunciaRepository denuncias;
    @Mock AnuncioRepository anuncios;
    @Mock UsuarioRepository usuarios;
    @Mock ModeracaoEventoRepository eventos;
    @InjectMocks DenunciaService service;

    @AfterEach void cleanup(){SecurityContextHolder.clearContext();}

    @Test void deniesDirectApiReportOnOwnListing(){
        Usuario dono=new Usuario();dono.setId(10L);dono.setStatus(StatusUsuario.ACTIVE);
        Anuncio anuncio=new Anuncio();anuncio.setId(20L);anuncio.setUsuario(dono);
        anuncio.setStatus(StatusAnuncio.ATIVO);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("10",null,java.util.List.of()));
        when(usuarios.findById(10L)).thenReturn(Optional.of(dono));
        when(anuncios.findById(20L)).thenReturn(Optional.of(anuncio));

        assertThrows(AcessoNegadoException.class,()->service.registrar(
                new DenunciaRequest(20L,CategoriaDenuncia.POSSIVEL_FRAUDE,
                        "Suspeita sem fundamento para teste")));
        verify(denuncias,never()).saveAndFlush(any());
    }
}
