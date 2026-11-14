package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.comercial.DeclararVendaRequest;
import com.josenetoo_dev.veiculos_api.dto.comercial.VendaDeclaradaResponse;
import com.josenetoo_dev.veiculos_api.enums.StatusAnuncio;
import com.josenetoo_dev.veiculos_api.exception.ex.AcessoNegadoException;
import com.josenetoo_dev.veiculos_api.exception.ex.AnuncioIndisponivelException;
import com.josenetoo_dev.veiculos_api.exception.ex.AnuncioNaoEncontradoException;
import com.josenetoo_dev.veiculos_api.model.Anuncio;
import com.josenetoo_dev.veiculos_api.model.AssinaturaVendedor;
import com.josenetoo_dev.veiculos_api.model.VendaDeclarada;
import com.josenetoo_dev.veiculos_api.repository.AnuncioRepository;
import com.josenetoo_dev.veiculos_api.repository.VendaDeclaradaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VendaDeclaradaService {
    private final AnuncioRepository anuncios;
    private final VendaDeclaradaRepository vendas;
    private final CurrentUserService currentUser;
    private final PlanoComercialService subscriptions;
    private final ContratoComercialService contracts;
    private final ComissaoCalculator commissionCalculator;

    @Transactional
    public VendaDeclaradaResponse declarar(Long anuncioId, DeclararVendaRequest request) {
        var seller = currentUser.current();
        Anuncio ad = anuncios.findByIdForUpdate(anuncioId)
                .orElseThrow(() -> new AnuncioNaoEncontradoException("Anúncio não encontrado"));
        if (ad.getUsuario() == null || !ad.getUsuario().getId().equals(seller.getId())) {
            throw new AcessoNegadoException("Somente o proprietário pode declarar esta venda");
        }
        if (ad.getStatus() != StatusAnuncio.ATIVO && ad.getStatus() != StatusAnuncio.PAUSADO) {
            throw new AnuncioIndisponivelException("Somente anúncios publicados ou pausados podem ser vendidos");
        }
        if (vendas.findByAnuncioId(anuncioId).isPresent()) {
            throw new AnuncioIndisponivelException("A venda deste anúncio já foi declarada");
        }
        AssinaturaVendedor subscription = subscriptions.assinaturaAtiva(seller.getId());
        if (!contracts.temAceitePublicado(seller.getId())) {
            throw new AnuncioIndisponivelException("Aceite a versão comercial vigente do contrato antes de declarar a venda");
        }
        VendaDeclarada sale = new VendaDeclarada();
        sale.setAnuncio(ad);
        sale.setVendedor(seller);
        sale.setAssinatura(subscription);
        sale.setPlanoCodigoSnapshot(subscription.getPlanoCodigoSnapshot());
        sale.setPlanoNomeSnapshot(subscription.getPlanoNomeSnapshot());
        sale.setValorFinal(request.valorFinal());
        sale.setComissaoPercentual(subscription.getComissaoPercentualSnapshot());
        sale.setComissaoCalculada(commissionCalculator.calculate(request.valorFinal(), subscription.getComissaoPercentualSnapshot()));
        sale.setDataVendaDeclarada(request.dataVenda());
        sale.setSituacao("PENDENTE_CONFERENCIA_COBRANCA_EXTERNA");
        return new VendaDeclaradaResponse(vendas.saveAndFlush(sale));
    }

    @Transactional(readOnly = true)
    public Page<VendaDeclaradaResponse> minhas(Pageable pageable) {
        return vendas.findByVendedorIdOrderByDeclaradoEmDesc(currentUser.current().getId(), pageable)
                .map(VendaDeclaradaResponse::new);
    }
}
