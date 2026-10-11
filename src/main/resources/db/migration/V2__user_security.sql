-- Aditiva: mantém IDs, contatos, hashes e relações existentes. Nenhum DROP/DELETE.
ALTER TABLE usuario ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
ALTER TABLE usuario ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE usuario ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;
