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
import java.math.BigDecimal;
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
        VendaDeclarada sale = new VendaDeclarada();
        sale.setAnuncio(ad);
        sale.setVendedor(seller);

        sale.setPlanoCodigoSnapshot("GRATUITO");
        sale.setPlanoNomeSnapshot("Publicação gratuita");
        sale.setValorFinal(request.valorFinal());
        sale.setComissaoPercentual(BigDecimal.ZERO);
        sale.setComissaoCalculada(BigDecimal.ZERO);
        sale.setDataVendaDeclarada(request.dataVenda());
        sale.setSituacao("DECLARADA");
        return new VendaDeclaradaResponse(vendas.saveAndFlush(sale));
    }

    @Transactional(readOnly = true)
    public Page<VendaDeclaradaResponse> minhas(Pageable pageable) {
        return vendas.findByVendedorIdOrderByDeclaradoEmDesc(currentUser.current().getId(), pageable)
                .map(VendaDeclaradaResponse::new);
    }
}
