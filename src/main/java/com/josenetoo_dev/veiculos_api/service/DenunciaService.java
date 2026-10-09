package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.denuncia.*;
import com.josenetoo_dev.veiculos_api.enums.*;
import com.josenetoo_dev.veiculos_api.exception.ex.*;
import com.josenetoo_dev.veiculos_api.model.*;
import com.josenetoo_dev.veiculos_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class DenunciaService {
    private final DenunciaRepository denuncias;
    private final AnuncioRepository anuncios;
    private final UsuarioRepository usuarios;
    private final ModeracaoEventoRepository eventos;

    private Usuario current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new CredenciaisInvalidasException("Não autenticado");
        }
        try {
            Usuario user = usuarios.findById(Long.valueOf(auth.getName()))
                .orElseThrow(() -> new CredenciaisInvalidasException("Conta inexistente"));
            if (user.getStatus() != StatusUsuario.ACTIVE) {
                throw new AcessoNegadoException("Confirme seu contato antes de registrar denúncia");
            }
            return user;
        } catch (NumberFormatException e) {
            throw new CredenciaisInvalidasException("Credenciais inválidas");
        }
    }

    private Usuario reviewer() {
        Usuario user = current();
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.REVIEWER) {
            throw new AcessoNegadoException("Permissão de revisão necessária");
        }
        return user;
    }

    @Transactional
    public DenunciaResponse registrar(DenunciaRequest request) {
        Usuario user = current();
        Anuncio ad = anuncios.findById(request.anuncioId())
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Anúncio não encontrado"));
        if (ad.getStatus() != StatusAnuncio.ATIVO) {
            throw new AnuncioNaoEncontradoException("Anúncio não está publicado");
        }
        if (ad.getUsuario().getId().equals(user.getId())) {
            throw new AcessoNegadoException("Não é possível denunciar o próprio anúncio");
        }
        if (denuncias.existsByAnuncioIdAndDenuncianteId(ad.getId(), user.getId())) {
            throw new VerificacaoIndisponivelException("Denúncia já registrada");
        }
        Denuncia item = new Denuncia();
        item.setAnuncio(ad);
        item.setDenunciante(user);
        item.setCategoria(request.categoria());
        item.setRelato(request.relato().strip());
        item.setStatus(StatusDenuncia.ABERTA);
        return DenunciaResponse.from(denuncias.saveAndFlush(item));
    }

    @Transactional(readOnly = true)
    public Page<DenunciaResponse> minhas(Pageable pageable) {
        return denuncias.findByDenuncianteId(current().getId(), pageable).map(DenunciaResponse::from);
    }

    @Transactional(readOnly = true)
    public DenunciaResponse consultar(Long id) {
        Usuario user = current();
        Denuncia item = denuncias.findById(id)
                .orElseThrow(() -> new DenunciaNaoEncontradaException());
        if (!item.getDenunciante().getId().equals(user.getId())
                && user.getRole() != Role.ADMIN && user.getRole() != Role.REVIEWER) {
            throw new AcessoNegadoException("Denúncia de outro usuário");
        }
        return DenunciaResponse.from(item);
    }

    @Transactional(readOnly = true)
    public Page<DenunciaResponse> pendentes(Pageable pageable) {
        reviewer();
        return denuncias.findByStatus(StatusDenuncia.ABERTA, pageable).map(DenunciaResponse::from);
    }

    @Transactional
    public DenunciaResponse decidir(Long id, boolean procedente, String motivo) {
        Usuario user = reviewer();
        if (motivo == null || motivo.isBlank() || motivo.strip().length() > 500) {
            throw new IllegalArgumentException("Justificativa obrigatória");
        }
        Denuncia report = denuncias.findByIdForUpdate(id)
                .orElseThrow(() -> new DenunciaNaoEncontradaException());
        if (report.getStatus() != StatusDenuncia.ABERTA) {
            throw new VerificacaoIndisponivelException("Denúncia já concluída");
        }
        if (report.getDenunciante().getId().equals(user.getId())
                || report.getAnuncio().getUsuario().getId().equals(user.getId())) {
            throw new AcessoNegadoException("Revisor não pode decidir denúncia em que está envolvido");
        }
        String reason = motivo.strip();
        if (procedente) {
            Anuncio ad = anuncios.findByIdForUpdate(report.getAnuncio().getId())
                    .orElseThrow(() -> new AnuncioNaoEncontradoException("Anúncio não encontrado"));
            if (ad.getStatus() == StatusAnuncio.ATIVO) {
                ad.setStatus(StatusAnuncio.PAUSADO);
                ad.setDestaque(false);
                anuncios.save(ad);
                eventos.save(new ModeracaoEvento(ad.getId(),user.getId(),
                        StatusAnuncio.ATIVO,StatusAnuncio.PAUSADO,reason));
            }
        }
        report.setStatus(procedente ? StatusDenuncia.CONFIRMADA : StatusDenuncia.DESCARTADA);
        report.setRevisadoPorId(user.getId());
        report.setRevisadoEm(LocalDateTime.now(ZoneOffset.UTC));
        report.setMotivoDecisao(reason);
        return DenunciaResponse.from(denuncias.save(report));
    }
}
