package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.exception.ex.AnuncioIndisponivelException;
import com.josenetoo_dev.veiculos_api.exception.ex.AnuncioNaoEncontradoException;
import com.josenetoo_dev.veiculos_api.model.ContratoVersao;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.AceiteContratoRepository;
import com.josenetoo_dev.veiculos_api.repository.ContratoVersaoRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContratoComercialServiceTest {
    private final ContratoVersaoRepository contracts = mock(ContratoVersaoRepository.class);
    private final AceiteContratoRepository acceptances = mock(AceiteContratoRepository.class);
    private final CurrentUserService currentUser = mock(CurrentUserService.class);
    private final ContratoComercialService service = new ContratoComercialService(contracts, acceptances, currentUser);

    @Test
    void doesNotExposeDraftAsCurrentContractWhenNoneIsPublished() {
        when(contracts.findFirstByPublicadoTrueOrderByCriadoEmDesc()).thenReturn(Optional.empty());

        assertThrows(AnuncioNaoEncontradoException.class, service::atual);
    }

    @Test
    void refusesAcceptanceOfUnpublishedContractVersion() {
        Usuario user = new Usuario();
        user.setId(7L);
        ContratoVersao draft = new ContratoVersao();
        draft.setId(9L);
        draft.setPublicado(false);
        when(currentUser.current()).thenReturn(user);
        when(contracts.findById(9L)).thenReturn(Optional.of(draft));

        assertThrows(AnuncioIndisponivelException.class, () -> service.aceitar(9L));
    }
}
