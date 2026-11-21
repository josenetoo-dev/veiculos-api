ALTER TABLE mensagem ADD COLUMN envio_proposta_id VARCHAR(36) NULL;
CREATE UNIQUE INDEX uk_mensagem_envio_proposta ON mensagem (proposta_id, envio_proposta_id);
