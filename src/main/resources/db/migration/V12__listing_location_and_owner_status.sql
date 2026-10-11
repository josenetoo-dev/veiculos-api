-- V12: localização declarada pelo anunciante e metadados de transição.
-- Somente ADD COLUMN / INDEX, mantendo IDs, propostas, documentos e imagens intactos.
-- Nullable para que registros antigos continuem válidos, sem inventar cidade/UF.
ALTER TABLE anuncio
    ADD COLUMN cidade VARCHAR(100) NULL,
    ADD COLUMN uf VARCHAR(2) NULL,
    ADD COLUMN status_atualizado_em DATETIME(6) NULL,
    ADD COLUMN status_atualizado_por_id BIGINT NULL;
CREATE INDEX idx_anuncio_uf_cidade ON anuncio(uf, cidade);
