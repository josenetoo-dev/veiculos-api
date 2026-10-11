-- Fase 3: revisão humana de identidade e evidências de propriedade.
-- Não substitui consulta oficial a CPF, CRLV, RENAVAM ou SENATRAN.
-- Arquivos em disco são criptografados (AES-256-GCM), referenciados por identificador aleatório.
-- Migration aditiva, sem exclusão de dados existentes.

CREATE TABLE verificacao (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 solicitante_id BIGINT NOT NULL,
 usuario_identidade_id BIGINT NULL,
 veiculo_id BIGINT NULL,
 tipo VARCHAR(20) NOT NULL,
 status VARCHAR(20) NOT NULL,
 motivo_rejeicao VARCHAR(500),
 criado_em DATETIME(6) NOT NULL,
 enviado_em DATETIME(6),
 revisado_em DATETIME(6),
 revisado_por_id BIGINT,
 CONSTRAINT fk_verificacao_solicitante FOREIGN KEY (solicitante_id) REFERENCES usuario(id),
 CONSTRAINT fk_verificacao_usuario FOREIGN KEY (usuario_identidade_id) REFERENCES usuario(id),
 CONSTRAINT fk_verificacao_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculo(id),
 CONSTRAINT uq_verificacao_usuario UNIQUE (usuario_identidade_id),
 CONSTRAINT uq_verificacao_veiculo UNIQUE (veiculo_id),
 CONSTRAINT chk_verificacao_objeto CHECK (
   (tipo='IDENTIDADE' AND usuario_identidade_id IS NOT NULL AND veiculo_id IS NULL)
   OR (tipo='VEICULO' AND veiculo_id IS NOT NULL AND usuario_identidade_id IS NULL)
 )
);

CREATE TABLE evidencia_verificacao (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 verificacao_id BIGINT NOT NULL,
 tipo VARCHAR(30) NOT NULL,
 arquivo_chave VARCHAR(36) NOT NULL,
 tipo_midia VARCHAR(24) NOT NULL,
 tamanho BIGINT NOT NULL,
 criado_em DATETIME(6) NOT NULL,
 CONSTRAINT fk_evidencia_verificacao FOREIGN KEY (verificacao_id) REFERENCES verificacao(id),
 CONSTRAINT uq_evidencia_tipo UNIQUE (verificacao_id, tipo),
 CONSTRAINT uq_evidencia_arquivo UNIQUE (arquivo_chave)
);

CREATE TABLE evento_verificacao (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 verificacao_id BIGINT NOT NULL,
 revisor_id BIGINT NOT NULL,
 resultado VARCHAR(20) NOT NULL,
 motivo VARCHAR(500),
 criado_em DATETIME(6) NOT NULL,
 INDEX idx_evento_verificacao_case (verificacao_id),
 INDEX idx_evento_verificacao_revisor (revisor_id)
);

CREATE INDEX idx_verificacao_solicitante ON verificacao(solicitante_id);
CREATE INDEX idx_verificacao_status ON verificacao(status);
