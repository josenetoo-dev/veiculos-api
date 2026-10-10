-- V10: desafio de recuperação de senha, armazenado somente como hash.
-- Migração aditiva: não altera credenciais ou estados de contas existentes.
ALTER TABLE usuario ADD COLUMN password_reset_token_hash VARCHAR(64) NULL;
ALTER TABLE usuario ADD COLUMN password_reset_expires_at DATETIME(6) NULL;
ALTER TABLE usuario ADD COLUMN password_reset_requested_at DATETIME(6) NULL;
ALTER TABLE usuario ADD COLUMN password_reset_failed_attempts INT NOT NULL DEFAULT 0;
