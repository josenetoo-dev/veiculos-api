-- Migração aditiva. NÃO executar em produção sem ensaio em banco restaurado.
ALTER TABLE usuario ADD COLUMN pending_email VARCHAR(254) NULL;
ALTER TABLE usuario ADD COLUMN pending_email_token_hash VARCHAR(64) NULL;
ALTER TABLE usuario ADD COLUMN pending_email_expires_at DATETIME(6) NULL;
ALTER TABLE usuario ADD COLUMN pending_email_requested_at DATETIME(6) NULL;
