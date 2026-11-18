package com.josenetoo_dev.veiculos_api.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Apenas notificações de mudança; o conteúdo permanece acessível somente por
 * GET autenticado de mensagens. Sem broker externo enquanto a API roda em uma
 * instância; clientes têm polling de recuperação para reconexões/deploys.
 */
@Component
public class ChatEvents {
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> listeners = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long propostaId) {
        SseEmitter emitter = new SseEmitter(60_000L);
        CopyOnWriteArrayList<SseEmitter> group = listeners.computeIfAbsent(
                propostaId, ignored -> new CopyOnWriteArrayList<>());
        group.add(emitter);
        Runnable cleanup = () -> {
            group.remove(emitter);
            if (group.isEmpty()) listeners.remove(propostaId, group);
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());
        try {
            emitter.send(SseEmitter.event().name("ready").data("connected"));
        } catch (Exception error) {
            cleanup.run();
            emitter.completeWithError(error);
        }
        return emitter;
    }

    /** Publica somente após commit, para nunca anunciar mensagem abortada. */
    public void afterCommit(Long propostaId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { broadcast(propostaId, "changed"); }
            });
        } else {
            broadcast(propostaId, "changed");
        }
    }

    @Scheduled(fixedDelay = 20_000L)
    public void heartbeat() {
        for (Long id : listeners.keySet()) broadcast(id, "heartbeat");
    }

    private void broadcast(Long propostaId, String name) {
        var group = listeners.get(propostaId);
        if (group == null) return;
        for (SseEmitter emitter : group) {
            try {
                emitter.send(SseEmitter.event().name(name).data("1"));
            } catch (Exception error) {
                group.remove(emitter);
                emitter.complete();
            }
        }
        if (group.isEmpty()) listeners.remove(propostaId, group);
    }
}
