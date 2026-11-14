ALTER TABLE usuario
 ADD COLUMN termos_versao VARCHAR(40) NULL,
 ADD COLUMN privacidade_versao VARCHAR(40) NULL,
 ADD COLUMN aceite_termos_em DATETIME(6) NULL;
CREATE TABLE solicitacao_privacidade (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 titular_id BIGINT NOT NULL,
 tipo VARCHAR(30) NOT NULL,
 status VARCHAR(30) NOT NULL,
 criado_em DATETIME(6) NOT NULL,
 CONSTRAINT fk_privacidade_titular FOREIGN KEY (titular_id) REFERENCES usuario(id),
 INDEX ix_privacidade_titular (titular_id, criado_em)
);
