-- Fase 2A: moderação obrigatória antes da publicação.
-- Migração aditiva (exceto reclassificação de anúncios legados ativos).
-- IMPORTANTE: ensaiar em backup restaurado antes de usar em produção.
ALTER TABLE anuncio ADD COLUMN revisado_em DATETIME(6) NULL;
ALTER TABLE anuncio ADD COLUMN revisado_por_id BIGINT NULL;
ALTER TABLE anuncio ADD COLUMN motivo_rejeicao VARCHAR(500) NULL;

CREATE TABLE moderacao_evento (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    anuncio_id BIGINT NOT NULL,
    revisor_id BIGINT NOT NULL,
    status_anterior VARCHAR(20) NOT NULL,
    status_novo VARCHAR(20) NOT NULL,
    motivo VARCHAR(500) NULL,
    criado_em DATETIME(6) NOT NULL,
    INDEX idx_moderacao_evento_anuncio (anuncio_id),
    INDEX idx_moderacao_evento_revisor (revisor_id)
);

-- Sem registro comprovado de aprovação anterior, anúncios ATIVO de versões
-- antigas deixam de ser públicos e aguardam a primeira revisão humana.
-- A operação preserva anúncios, IDs, propostas, mídias e demais registros.
UPDATE anuncio SET status = 'PENDENTE' WHERE status = 'ATIVO';
