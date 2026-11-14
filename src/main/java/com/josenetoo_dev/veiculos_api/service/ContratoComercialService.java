package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.comercial.*;
import com.josenetoo_dev.veiculos_api.exception.ex.AnuncioIndisponivelException;
import com.josenetoo_dev.veiculos_api.exception.ex.AnuncioNaoEncontradoException;
import com.josenetoo_dev.veiculos_api.model.AceiteContrato;
import com.josenetoo_dev.veiculos_api.model.ContratoVersao;
import com.josenetoo_dev.veiculos_api.repository.AceiteContratoRepository;
import com.josenetoo_dev.veiculos_api.repository.ContratoVersaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContratoComercialService {
    private final ContratoVersaoRepository contratos;
    private final AceiteContratoRepository aceites;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public ContratoResponse atual() {
        ContratoVersao version = contratos.findFirstByPublicadoTrueOrderByCriadoEmDesc()
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Contrato indisponível"));
        verificarIntegridade(version);
        return new ContratoResponse(version);
    }

    @Transactional(readOnly = true)
    public List<AceiteContratoResponse> meusAceites() {
        return aceites.findByUsuarioIdOrderByAceitoEmDesc(currentUser.current().getId()).stream()
                .map(AceiteContratoResponse::new).toList();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ContratoResponse> versoesAdmin(org.springframework.data.domain.Pageable pageable) {
        currentUser.admin();
        return contratos.findAll(pageable).map(ContratoResponse::new);
    }

    @Transactional
    public AceiteContratoResponse aceitar(Long contratoId) {
        var user = currentUser.current();
        if (user.getCpfCiphertext() == null || user.getCpfCiphertext().isBlank()) {
            throw new AnuncioIndisponivelException("Informe nome completo e CPF protegido antes de aceitar o contrato");
        }
        ContratoVersao version = contratos.findById(contratoId)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Contrato não encontrado"));
        if (!version.isPublicado()) {
            throw new AnuncioIndisponivelException("Somente uma versão publicada pode ser aceita");
        }
        verificarIntegridade(version);
        return aceites.findByUsuarioIdAndContratoId(user.getId(), contratoId)
                .map(AceiteContratoResponse::new)
                .orElseGet(() -> {
                    try {
                        AceiteContrato acceptance = aceites.saveAndFlush(new AceiteContrato(user, version, version.getHashSha256()));
                        return new AceiteContratoResponse(acceptance);
                    } catch (DataIntegrityViolationException race) {
                        return new AceiteContratoResponse(aceites.findByUsuarioIdAndContratoId(user.getId(), contratoId).orElseThrow());
                    }
                });
    }

    @Transactional
    public ContratoResponse criar(CriarContratoRequest request) {
        currentUser.admin();
        String content = request.conteudo().strip();
        if (content.isBlank()) throw new IllegalArgumentException("Conteúdo do contrato obrigatório");
        ContratoVersao version = new ContratoVersao();
        version.setVersao(request.versao().strip());
        version.setTitulo(request.titulo().strip());
        version.setConteudo(content);
        version.setHashSha256(hash(content));
        version.setPublicado(false);
        return new ContratoResponse(contratos.saveAndFlush(version));
    }

    @Transactional
    public ContratoResponse publicar(Long id) {
        currentUser.admin();
        ContratoVersao version = contratos.findById(id)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Contrato não encontrado"));
        verificarIntegridade(version);
        if (version.getConteudo().contains("{{") || version.getConteudo().toUpperCase().contains("MINUTA DEMONSTRATIVA")) {
            throw new AnuncioIndisponivelException("Preencha os dados jurídicos e publique uma versão revisada antes de ativar o contrato");
        }
        contratos.findFirstByPublicadoTrueOrderByCriadoEmDesc().ifPresent(current -> current.setPublicado(false));
        version.setPublicado(true);
        return new ContratoResponse(contratos.saveAndFlush(version));
    }

    @Transactional(readOnly = true)
    public boolean temAceitePublicado(Long userId) {
        return contratos.findFirstByPublicadoTrueOrderByCriadoEmDesc()
                .filter(version -> aceites.findByUsuarioIdAndContratoId(userId, version.getId()).isPresent())
                .isPresent();
    }

    private void verificarIntegridade(ContratoVersao version) {
        if (!MessageDigest.isEqual(version.getHashSha256().getBytes(StandardCharsets.US_ASCII),
                hash(version.getConteudo()).getBytes(StandardCharsets.US_ASCII))) {
            throw new IllegalStateException("A integridade do contrato não foi confirmada");
        }
    }

    private static String hash(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 indisponível", impossible);
        }
    }
}
