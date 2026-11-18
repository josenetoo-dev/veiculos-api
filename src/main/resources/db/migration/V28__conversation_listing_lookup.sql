-- Índice aditivo: preserva todas as propostas e mensagens anteriores.
-- Não criar UNIQUE nesta etapa: possíveis duplicatas legadas requerem
-- reconciliação auditada, jamais exclusão ou reatribuição silenciosa.
CREATE INDEX idx_proposta_anuncio_comprador_id ON proposta (anunciante_id, comprador_id, id);
