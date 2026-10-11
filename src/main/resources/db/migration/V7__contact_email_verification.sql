-- V7: verificação do endereço de contato; não substitui avaliação documental.
-- Migração aditiva: mantém todas as contas e suas credenciais.
ALTER TABLE usuario ADD COLUMN contact_email_token_hash VARCHAR(64) NULL;
ALTER TABLE usuario ADD COLUMN contact_email_expires_at DATETIME(6) NULL;
ALTER TABLE usuario ADD COLUMN contact_email_requested_at DATETIME(6) NULL;
ALTER TABLE usuario ADD COLUMN contact_email_verified_at DATETIME(6) NULL;
