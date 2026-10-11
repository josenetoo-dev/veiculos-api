-- Apenas bancos vazios. Bancos existentes exigem baseline MANUAL na versão 1
-- após verificar schema/backup; baseline-on-migrate permanece false.
CREATE TABLE usuario (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 nome VARCHAR(255) NOT NULL,
 email VARCHAR(255) NOT NULL UNIQUE,
 senha VARCHAR(255) NOT NULL,
 telefone VARCHAR(255) NOT NULL,
 criado_em DATETIME(6)
);
CREATE TABLE anuncio (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 codigo VARCHAR(255) UNIQUE,
 versao VARCHAR(255) NOT NULL,
 destaque BOOLEAN NOT NULL,
 laudo_cautelar VARCHAR(255),
 documentacao VARCHAR(255) NOT NULL,
 garantia VARCHAR(255) NOT NULL,
 titulo VARCHAR(255) NOT NULL,
 descricao VARCHAR(255) NOT NULL,
 preco DECIMAL(38,2) NOT NULL,
 marca VARCHAR(255) NOT NULL,
 modelo VARCHAR(255) NOT NULL,
 ano INT NOT NULL,
 quilometragem INT NOT NULL,
 cor VARCHAR(255) NOT NULL,
 combustivel ENUM('GASOLINA','ETANOL','FLEX','DIESEL','ELETRICO','HIBRIDO') NOT NULL,
 segunda_mao BOOLEAN NOT NULL,
 status VARCHAR(20) NOT NULL,
 cambio ENUM('AUTOMATICO','MANUAL','AUTOMATIZADO') NOT NULL,
 categoria ENUM('SEMINOVOS','PREMIUM','BLINDADOS','MOTOS','CONSIGNACAO') NOT NULL,
 criado_em DATETIME(6) NOT NULL,
 usuario_id BIGINT,
 CONSTRAINT fk_anuncio_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);
CREATE TABLE anuncio_foto (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 anuncio_id BIGINT NOT NULL,
 url VARCHAR(255) NOT NULL,
 ordem INT NOT NULL,
 tipo_foto ENUM('FRENTE','TRASEIRA','INTERIOR','PAINEL','OUTRO') NOT NULL,
 CONSTRAINT fk_foto_anuncio FOREIGN KEY (anuncio_id) REFERENCES anuncio(id)
);
CREATE TABLE proposta (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 valor DECIMAL(38,2) NOT NULL,
 descricao VARCHAR(255) NOT NULL,
 status VARCHAR(20) NOT NULL,
 criado_em DATETIME(6) NOT NULL,
 contraproposta_valor DECIMAL(38,2),
 contraproposta_descricao VARCHAR(255),
 contraproposta_feita BOOLEAN NOT NULL,
 anunciante_id BIGINT,
 comprador_id BIGINT,
 CONSTRAINT fk_proposta_anuncio FOREIGN KEY (anunciante_id) REFERENCES anuncio(id),
 CONSTRAINT fk_proposta_comprador FOREIGN KEY (comprador_id) REFERENCES usuario(id)
);
CREATE TABLE mensagem (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 conteudo VARCHAR(2000) NOT NULL,
 criado_em DATETIME(6) NOT NULL,
 proposta_id BIGINT NOT NULL,
 remetente_id BIGINT NOT NULL,
 CONSTRAINT fk_mensagem_proposta FOREIGN KEY (proposta_id) REFERENCES proposta(id),
 CONSTRAINT fk_mensagem_remetente FOREIGN KEY (remetente_id) REFERENCES usuario(id)
);
