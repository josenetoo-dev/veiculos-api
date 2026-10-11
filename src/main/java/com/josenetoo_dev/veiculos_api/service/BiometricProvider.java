package com.josenetoo_dev.veiculos_api.service;

/**
 * Fronteira auditável AWS. Nenhum método aceita resultado informado pelo browser:
 * o backend consulta a AWS para confirmar a sessão e comparar as imagens.
 */
public interface BiometricProvider {
    enum Outcome { AGUARDANDO, APROVADA_TECNICAMENTE, INCONCLUSIVA }
    String criarSessao();
    Outcome consultar(String sessionId, byte[] documentoFrente, float limiarVida, float limiarFace);
}
