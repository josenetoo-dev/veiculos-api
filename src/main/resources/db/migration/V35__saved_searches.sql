CREATE TABLE IF NOT EXISTS busca_salva (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    nome VARCHAR(60) NOT NULL,
    filtros_json JSON NOT NULL,
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    atualizado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_busca_salva_usuario_data (usuario_id, atualizado_em),
    CONSTRAINT fk_busca_salva_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
);
