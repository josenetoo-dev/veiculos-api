-- V8: arquivamento reversível por implementação futura; sem perda de negociações.
-- A operação de desativação passa a manter anúncio, propostas, mensagens e auditoria.
ALTER TABLE anuncio ADD COLUMN arquivado_em DATETIME(6) NULL;
ALTER TABLE anuncio ADD COLUMN arquivado_por_id BIGINT NULL;
