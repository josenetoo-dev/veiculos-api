-- Fase 2B: transição sem perda de dados para cadastro de veículo independente.
-- Ainda preservamos as colunas legadas de anuncio por compatibilidade com rollback.
-- NÃO executar em produção antes de ensaiar um backup restaurado de MySQL.
CREATE TABLE veiculo (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 cadastrado_por_id BIGINT,
 marca VARCHAR(255) NOT NULL,
 modelo VARCHAR(255) NOT NULL,
 versao VARCHAR(255) NOT NULL,
 ano INT NOT NULL,
 quilometragem INT NOT NULL,
 cor VARCHAR(255) NOT NULL,
 combustivel ENUM('GASOLINA','ETANOL','FLEX','DIESEL','ELETRICO','HIBRIDO') NOT NULL,
 cambio ENUM('AUTOMATICO','MANUAL','AUTOMATIZADO') NOT NULL,
 segunda_mao BOOLEAN NOT NULL,
 CONSTRAINT fk_veiculo_cadastrado_por FOREIGN KEY (cadastrado_por_id) REFERENCES usuario(id)
);

ALTER TABLE anuncio ADD COLUMN veiculo_id BIGINT NULL;

-- Um veículo inicial para cada anúncio legado, mantendo os mesmos IDs por
-- simplicidade da reconciliação. Não confundir veiculo.id com placa/RENAVAM.
INSERT INTO veiculo (id,cadastrado_por_id,marca,modelo,versao,ano,quilometragem,cor,combustivel,cambio,segunda_mao)
SELECT id,usuario_id,marca,modelo,versao,ano,quilometragem,cor,combustivel,cambio,segunda_mao
FROM anuncio;

UPDATE anuncio SET veiculo_id = id;

-- Falha se existir anúncio sem vínculo: não deixar dados silenciosamente órfãos.
ALTER TABLE anuncio MODIFY COLUMN veiculo_id BIGINT NOT NULL;
ALTER TABLE anuncio ADD CONSTRAINT fk_anuncio_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculo(id);
CREATE INDEX idx_anuncio_veiculo ON anuncio(veiculo_id);

-- As colunas antigas permanecem na tabela anuncio como espelho
-- transicional. Nenhum DROP/DELETE nem mudança no contrato HTTP.
