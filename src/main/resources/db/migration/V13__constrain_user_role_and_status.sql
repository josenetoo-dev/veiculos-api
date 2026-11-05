-- Defaults para inserts legados e restrições contra valores não representáveis pelos enums Java.
-- Não altera linhas existentes; registros incompatíveis devem ser auditados antes da migration.
ALTER TABLE usuario
    ALTER COLUMN role SET DEFAULT 'USER',
    ALTER COLUMN status SET DEFAULT 'PENDING_CONTACT_VERIFICATION';

ALTER TABLE usuario
    ADD CONSTRAINT chk_usuario_role
        CHECK (role IN ('USER', 'REVIEWER', 'ADMIN')),
    ADD CONSTRAINT chk_usuario_status
        CHECK (status IN ('PENDING_CONTACT_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'BLOCKED', 'DELETED'));
