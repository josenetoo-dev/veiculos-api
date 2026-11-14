ALTER TABLE mensagem
    ADD COLUMN excluida_em DATETIME NULL,
    ADD COLUMN excluida_por_id BIGINT NULL,
    ADD CONSTRAINT fk_mensagem_excluida_por FOREIGN KEY (excluida_por_id) REFERENCES usuario(id);

CREATE INDEX idx_mensagem_proposta_excluida_criada
    ON mensagem (proposta_id, excluida_em, criado_em);
