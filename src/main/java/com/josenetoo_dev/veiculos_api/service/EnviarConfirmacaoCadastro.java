package com.josenetoo_dev.veiculos_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EnviarConfirmacaoCadastro {
    private final ContatoEmailService contato;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EnviarConfirmacaoCadastro.class);

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enviar(ContaCriada event) {
        try {
            contato.solicitarCadastro(event.usuarioId());
        } catch (RuntimeException failure) {
            log.warn("Envio da confirmação indisponível. Titular pode solicitar reenvio.");
        }
    }
}
