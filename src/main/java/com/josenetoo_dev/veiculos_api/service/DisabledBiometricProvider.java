package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.exception.ex.BiometriaIndisponivelException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Ambiente sem AWS configurada: não faz reconhecimento simulado nem cobra. */
@Component
@ConditionalOnProperty(prefix="verification.biometric",name="enabled",havingValue="false",matchIfMissing=true)
public class DisabledBiometricProvider implements BiometricProvider {
    @Override public String criarSessao() { throw new BiometriaIndisponivelException(); }
    @Override public Outcome consultar(String sessionId, byte[] foto, float vida, float face) {
        throw new BiometriaIndisponivelException();
    }
}
