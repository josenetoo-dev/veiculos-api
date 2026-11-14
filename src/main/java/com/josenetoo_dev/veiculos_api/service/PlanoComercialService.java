package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.comercial.*;
import com.josenetoo_dev.veiculos_api.enums.StatusAssinatura;
import com.josenetoo_dev.veiculos_api.exception.ex.AnuncioIndisponivelException;
import com.josenetoo_dev.veiculos_api.exception.ex.AnuncioNaoEncontradoException;
import com.josenetoo_dev.veiculos_api.model.AssinaturaVendedor;
import com.josenetoo_dev.veiculos_api.model.EventoAssinatura;
import com.josenetoo_dev.veiculos_api.model.PlanoComercial;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanoComercialService {
    private final PlanoComercialRepository planos;
    private final AssinaturaVendedorRepository assinaturas;
    private final EventoAssinaturaRepository eventos;
    private final CurrentUserService currentUser;
    private final tools.jackson.databind.ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<PlanoResponse> listarPlanos() {
        return planos.findByAtivoTrueOrderByMensalidadeAsc().stream().map(PlanoResponse::new).toList();
    }

    @Transactional
    public PlanoResponse atualizarBeneficios(String codigo, AtualizarBeneficiosRequest request) {
        currentUser.admin();
        var plan = planos.findByCodigo(codigo == null ? "" : codigo.strip().toUpperCase())
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Plano não encontrado"));
        List<String> benefits = request.beneficios() == null ? List.of() : request.beneficios().stream()
                .map(String::strip).distinct().toList();
        try {
            plan.setBeneficiosJson(objectMapper.writeValueAsString(benefits));
        } catch (tools.jackson.core.JacksonException impossible) {
            throw new IllegalStateException("Não foi possível salvar os benefícios configurados", impossible);
        }
        plan.setAtualizadoEm(LocalDateTime.now(ZoneOffset.UTC));
        return new PlanoResponse(planos.saveAndFlush(plan));
    }

    @Transactional
    public AssinaturaResponse registrarIntencao(IntencaoAssinaturaRequest request) {
        Usuario user = currentUser.current();
        exigirIdentificacao(user);
        PlanoComercial plan = planos.findByCodigoAndAtivoTrue(request.codigoPlano().trim().toUpperCase())
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Plano não encontrado"));
        AssinaturaVendedor subscription = new AssinaturaVendedor();
        subscription.setVendedor(user);
        subscription.snapshot(plan);
        subscription.setStatus(StatusAssinatura.INTENCAO);
        subscription.setSituacaoFinanceira("PENDENTE_EXTERNO");
        subscription = assinaturas.saveAndFlush(subscription);
        eventos.save(new EventoAssinatura(subscription, user, "INTENCAO_REGISTRADA",
                "Cobrança externa não confirmada; assinatura permanece inativa"));
        return new AssinaturaResponse(subscription);
    }

    private void exigirIdentificacao(Usuario user) {
        if (user.getCpfCiphertext() == null || user.getCpfCiphertext().isBlank()) {
            throw new com.josenetoo_dev.veiculos_api.exception.ex.AnuncioIndisponivelException(
                    "Informe nome completo e CPF protegido antes de contratar um plano");
        }
    }

    @Transactional(readOnly = true)
    public Page<AssinaturaResponse> minhas(Pageable pageable) {
        return assinaturas.findByVendedorIdOrderByCriadoEmDesc(currentUser.current().getId(), pageable)
                .map(AssinaturaResponse::new);
    }

    @Transactional(readOnly = true)
    public Page<AssinaturaResponse> todas(Pageable pageable) {
        currentUser.admin();
        return assinaturas.findAll(pageable).map(AssinaturaResponse::new);
    }

    @Transactional
    public AssinaturaResponse cancelar(Long id) {
        Usuario user = currentUser.current();
        AssinaturaVendedor subscription = assinaturas.findByIdForUpdate(id)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Assinatura não encontrada"));
        if (!subscription.getVendedor().getId().equals(user.getId())) {
            throw new com.josenetoo_dev.veiculos_api.exception.ex.AcessoNegadoException("Assinatura de outro vendedor");
        }
        if (subscription.getStatus() == StatusAssinatura.CANCELADA || subscription.getStatus() == StatusAssinatura.EXPIRADA) {
            throw new AnuncioIndisponivelException("Assinatura já encerrada");
        }
        subscription.setStatus(StatusAssinatura.CANCELADA);
        subscription.setCanceladaEm(LocalDateTime.now(ZoneOffset.UTC));
        subscription = assinaturas.saveAndFlush(subscription);
        eventos.save(new EventoAssinatura(subscription, user, "CANCELADA", "Cancelamento registrado pelo vendedor"));
        return new AssinaturaResponse(subscription);
    }

    @Transactional(readOnly = true)
    public List<EventoAssinaturaResponse> historico(Long id) {
        Usuario user = currentUser.current();
        AssinaturaVendedor subscription = assinaturas.findById(id)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Assinatura não encontrada"));
        if (!subscription.getVendedor().getId().equals(user.getId())
                && user.getRole() != com.josenetoo_dev.veiculos_api.enums.Role.ADMIN
                && user.getRole() != com.josenetoo_dev.veiculos_api.enums.Role.REVIEWER) {
            throw new com.josenetoo_dev.veiculos_api.exception.ex.AcessoNegadoException("Histórico de outra assinatura");
        }
        return eventos.findByAssinaturaIdOrderByCriadoEmAsc(id).stream().map(EventoAssinaturaResponse::new).toList();
    }

    @Transactional
    public AssinaturaResponse confirmarPagamentoExterno(Long id, ConfirmarPagamentoRequest request) {
        Usuario admin = currentUser.admin();
        AssinaturaVendedor subscription = assinaturas.findByIdForUpdate(id)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Assinatura não encontrada"));
        if (subscription.getStatus() != StatusAssinatura.INTENCAO
                && subscription.getStatus() != StatusAssinatura.PENDENTE_PAGAMENTO_EXTERNO) {
            throw new AnuncioIndisponivelException("A assinatura não aguarda confirmação externa");
        }
        String reference = request.referenciaExterna().strip();
        if (reference.length() < 4) throw new IllegalArgumentException("Referência externa inválida");
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (AssinaturaVendedor previous : assinaturas.findByVendedorIdAndStatusOrderByCriadoEmDesc(
                subscription.getVendedor().getId(), StatusAssinatura.ATIVA)) {
            if (!previous.getId().equals(subscription.getId())) {
                previous.setStatus(StatusAssinatura.CANCELADA);
                previous.setCanceladaEm(now);
                eventos.save(new EventoAssinatura(previous, admin, "SUBSTITUIDA_POR_NOVO_PLANO",
                        "Plano encerrado após confirmação externa de uma nova contratação; nenhum reembolso foi processado"));
            }
        }
        subscription.setStatus(StatusAssinatura.ATIVA);
        subscription.setSituacaoFinanceira("CONFIRMADO_EXTERNAMENTE");
        subscription.setInicioVigencia(now);
        subscription.setVencimentoEm(now.plusMonths(1));
        subscription.setConfirmadaPor(admin);
        subscription.setReferenciaPagamentoExterno(reference);
        subscription = assinaturas.saveAndFlush(subscription);
        eventos.save(new EventoAssinatura(subscription, admin, "PAGAMENTO_EXTERNO_CONFIRMADO",
                "Confirmação manual; referência externa registrada"));
        return new AssinaturaResponse(subscription);
    }

    @Transactional(readOnly = true)
    public AssinaturaVendedor assinaturaAtiva(Long vendedorId) {
        return assinaturas.findFirstByVendedorIdAndStatusOrderByCriadoEmDesc(vendedorId, StatusAssinatura.ATIVA)
                .filter(s -> s.getVencimentoEm() != null && s.getVencimentoEm().isAfter(LocalDateTime.now(ZoneOffset.UTC)))
                .orElseThrow(() -> new AnuncioIndisponivelException("É necessária uma assinatura ativa e vigente"));
    }
}
