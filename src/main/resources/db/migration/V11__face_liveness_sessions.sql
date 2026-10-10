-- V11: sessões de comparação facial com prova de vida.
-- Armazena somente status/consentimento/id da sessão e vínculo ao documento.
-- NÃO guarda imagens, vídeos, embeddings ou scores biométricos.
CREATE TABLE sessao_biometria (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 verificacao_id BIGINT NOT NULL,
 documento_evidencia_id BIGINT NOT NULL,
 status VARCHAR(30) NOT NULL,
 criado_em DATETIME(6) NOT NULL,
 finalizado_em DATETIME(6) NULL,
 aceite_biometria_em DATETIME(6) NOT NULL,
 versao_politica VARCHAR(60) NOT NULL,
 CONSTRAINT fk_biometria_verificacao FOREIGN KEY (verificacao_id) REFERENCES verificacao(id),
 INDEX idx_biometria_verificacao_criado (verificacao_id, criado_em)
);

-- Recurso de acessibilidade/contestação: dispensa por ADMIN com justificativa.
-- Este evento NÃO implica aprovação da identidade, só acesso à revisão humana.
CREATE TABLE dispensa_biometria (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 verificacao_id BIGINT NOT NULL,
 documento_evidencia_id BIGINT NOT NULL,
 revisor_id BIGINT NOT NULL,
 motivo VARCHAR(500) NOT NULL,
 criado_em DATETIME(6) NOT NULL,
 CONSTRAINT fk_dispensa_biometria_verificacao FOREIGN KEY (verificacao_id) REFERENCES verificacao(id),
 CONSTRAINT uq_dispensa_documento UNIQUE (verificacao_id, documento_evidencia_id)
);
