package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.anuncio_dto.AnuncioResponse;
import com.josenetoo_dev.veiculos_api.enums.StatusAnuncio;
import com.josenetoo_dev.veiculos_api.enums.StatusUsuario;
import com.josenetoo_dev.veiculos_api.exception.ex.*;
import com.josenetoo_dev.veiculos_api.model.Anuncio;
import com.josenetoo_dev.veiculos_api.model.ModeracaoEvento;
import com.josenetoo_dev.veiculos_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
public class ModeracaoService {
    private final AnuncioRepository anuncios;
    private final UsuarioRepository usuarios;
    private final ModeracaoEventoRepository eventos;

    private Long revisorId() {
        try {
            return Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        } catch (NullPointerException | NumberFormatException e) {
            throw new CredenciaisInvalidasException("Revisor não autenticado");
        }
    }

    @Transactional(readOnly = true)
    public Page<AnuncioResponse> pendentes(Pageable pageable) {
        return anuncios.findByStatus(StatusAnuncio.PENDENTE, pageable).map(AnuncioResponse::new);
    }

    @Transactional(readOnly = true)
    public AnuncioResponse consultar(Long id) {
        Anuncio anuncio = anuncios.findById(id)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Anúncio não encontrado"));
        return new AnuncioResponse(anuncio);
    }

    private Anuncio obterPendenteParaRevisao(Long id, Long revisorId) {
        Anuncio anuncio = anuncios.findByIdForUpdate(id)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Anúncio não encontrado"));
        if (anuncio.getStatus() != StatusAnuncio.PENDENTE) {
            throw new AnuncioIndisponivelException("Anúncio não está pendente de revisão");
        }
        if (anuncio.getUsuario().getId().equals(revisorId)) {
            throw new AcessoNegadoException("Um revisor não pode aprovar o próprio anúncio");
        }
        return anuncio;
    }

    @Transactional
    public AnuncioResponse aprovar(Long id) {
        Long revisorId = revisorId();
        Anuncio anuncio = obterPendenteParaRevisao(id, revisorId);
        if (anuncio.getUsuario().getStatus() != StatusUsuario.ACTIVE) {
            throw new AnuncioIndisponivelException("Conta do vendedor ainda não está ativa");
        }

        anuncio.setStatus(StatusAnuncio.ATIVO);
        anuncio.setRevisadoEm(LocalDateTime.now(ZoneOffset.UTC));
        anuncio.setRevisadoPorId(revisorId);
        anuncio.setMotivoRejeicao(null);
        eventos.save(new ModeracaoEvento(id, revisorId, StatusAnuncio.PENDENTE, StatusAnuncio.ATIVO, null));
        return new AnuncioResponse(anuncios.save(anuncio));
    }

    @Transactional
    public AnuncioResponse rejeitar(Long id, String motivo) {
        if (motivo == null || motivo.isBlank() || motivo.length() > 500) {
            throw new IllegalArgumentException("Motivo de rejeição inválido");
        }
        Long revisorId = revisorId();
        Anuncio anuncio = obterPendenteParaRevisao(id, revisorId);
        String motivoLimpo = motivo.strip();

        anuncio.setStatus(StatusAnuncio.REJEITADO);
        anuncio.setRevisadoEm(LocalDateTime.now(ZoneOffset.UTC));
        anuncio.setRevisadoPorId(revisorId);
        anuncio.setMotivoRejeicao(motivoLimpo);
        eventos.save(new ModeracaoEvento(id, revisorId, StatusAnuncio.PENDENTE, StatusAnuncio.REJEITADO, motivoLimpo));
        return new AnuncioResponse(anuncios.save(anuncio));
    }
}
