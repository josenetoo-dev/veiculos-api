-- Denúncias de anúncio revisadas manualmente; sem remoção automática.
CREATE TABLE denuncia (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 anuncio_id BIGINT NOT NULL,
 denunciante_id BIGINT NOT NULL,
 categoria VARCHAR(30) NOT NULL,
 relato VARCHAR(1000) NOT NULL,
 status VARCHAR(20) NOT NULL,
 criado_em DATETIME(6) NOT NULL,
 revisado_em DATETIME(6),
 revisado_por_id BIGINT,
 motivo_decisao VARCHAR(500),
 CONSTRAINT fk_denuncia_anuncio FOREIGN KEY (anuncio_id) REFERENCES anuncio(id),
 CONSTRAINT fk_denuncia_usuario FOREIGN KEY (denunciante_id) REFERENCES usuario(id),
 CONSTRAINT uq_denuncia_anuncio_usuario UNIQUE (anuncio_id,denunciante_id),
 INDEX idx_denuncia_status (status)
);
