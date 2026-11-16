package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.enums.StatusAnuncio;
import com.josenetoo_dev.veiculos_api.enums.StatusAssinatura;
import com.josenetoo_dev.veiculos_api.repository.AnuncioRepository;
import com.josenetoo_dev.veiculos_api.repository.AssinaturaVendedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class ExpiracaoComercialService {
    private final AssinaturaVendedorRepository assinaturas;
    private final AnuncioRepository anuncios;

    @Scheduled(fixedDelayString = "${app.commercial.expiration-interval-ms:60000}")
    @Transactional
    public void expirar() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (var candidate : assinaturas.findTop100ByStatusAndVencimentoEmBefore(StatusAssinatura.ATIVA, now)) {
            var subscription = assinaturas.findByIdForUpdate(candidate.getId()).orElseThrow();
            if (subscription.getStatus() != StatusAssinatura.ATIVA || !subscription.getVencimentoEm().isBefore(now)) continue;
            subscription.setStatus(StatusAssinatura.EXPIRADA);
            for (var ad : anuncios.findByAssinaturaPublicacaoIdAndStatus(subscription.getId(), StatusAnuncio.ATIVO)) {
                ad.setStatus(StatusAnuncio.PAUSADO);
                ad.setStatusAtualizadoEm(now);
            }
        }
    }
}
