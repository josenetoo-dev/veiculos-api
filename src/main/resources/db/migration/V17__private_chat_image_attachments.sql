ALTER TABLE mensagem
    ADD COLUMN arquivo_chave VARCHAR(36) NULL,
    ADD COLUMN arquivo_nome VARCHAR(120) NULL,
    ADD COLUMN tipo_midia VARCHAR(24) NULL,
    ADD COLUMN arquivo_tamanho BIGINT NULL,
    ADD CONSTRAINT uq_mensagem_arquivo_chave UNIQUE (arquivo_chave),
    ADD CONSTRAINT chk_mensagem_anexo_metadata CHECK (
      (arquivo_chave IS NULL AND arquivo_nome IS NULL AND tipo_midia IS NULL AND arquivo_tamanho IS NULL)
      OR
      (arquivo_chave IS NOT NULL AND arquivo_nome IS NOT NULL AND tipo_midia IN ('image/jpeg','image/png') AND arquivo_tamanho > 0)
    );
