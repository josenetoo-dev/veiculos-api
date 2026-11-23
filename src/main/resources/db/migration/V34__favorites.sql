CREATE TABLE IF NOT EXISTS anuncio_favorito (
    usuario_id BIGINT NOT NULL,
    anuncio_id BIGINT NOT NULL,
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (usuario_id, anuncio_id),
    KEY idx_favorito_usuario_data (usuario_id, criado_em),
    CONSTRAINT fk_favorito_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorito_anuncio FOREIGN KEY (anuncio_id) REFERENCES anuncio(id) ON DELETE CASCADE
);
