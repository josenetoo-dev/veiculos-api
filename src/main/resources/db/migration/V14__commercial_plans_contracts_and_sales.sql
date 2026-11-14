CREATE TABLE plano_comercial (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(30) NOT NULL,
  nome VARCHAR(80) NOT NULL,
  mensalidade DECIMAL(10,2) NOT NULL,
  comissao_percentual DECIMAL(5,2) NOT NULL,
  beneficios_json LONGTEXT NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  criado_em DATETIME(6) NOT NULL,
  atualizado_em DATETIME(6) NOT NULL,
  CONSTRAINT uq_plano_comercial_codigo UNIQUE (codigo),
  CONSTRAINT chk_plano_mensalidade CHECK (mensalidade >= 0),
  CONSTRAINT chk_plano_comissao CHECK (comissao_percentual >= 0 AND comissao_percentual <= 100)
);

INSERT INTO plano_comercial (codigo,nome,mensalidade,comissao_percentual,beneficios_json,ativo,criado_em,atualizado_em)
VALUES
 ('BASIC','Básico',75.00,0.65,'[]',TRUE,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)),
 ('PROFESSIONAL','Profissional',125.00,0.65,'[]',TRUE,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)),
 ('PREMIUM','Premium',175.00,0.50,'[]',TRUE,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6));

CREATE TABLE assinatura_vendedor (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  vendedor_id BIGINT NOT NULL,
  plano_id BIGINT NOT NULL,
  plano_codigo_snapshot VARCHAR(30) NOT NULL,
  plano_nome_snapshot VARCHAR(80) NOT NULL,
  mensalidade_snapshot DECIMAL(10,2) NOT NULL,
  comissao_percentual_snapshot DECIMAL(5,2) NOT NULL,
  status VARCHAR(30) NOT NULL,
  situacao_financeira VARCHAR(30) NOT NULL,
  criado_em DATETIME(6) NOT NULL,
  inicio_vigencia DATETIME(6) NULL,
  vencimento_em DATETIME(6) NULL,
  cancelada_em DATETIME(6) NULL,
  confirmada_por_id BIGINT NULL,
  referencia_pagamento_externo VARCHAR(160) NULL,
  CONSTRAINT fk_assinatura_vendedor FOREIGN KEY (vendedor_id) REFERENCES usuario(id),
  CONSTRAINT fk_assinatura_plano FOREIGN KEY (plano_id) REFERENCES plano_comercial(id),
  CONSTRAINT fk_assinatura_confirmada_por FOREIGN KEY (confirmada_por_id) REFERENCES usuario(id),
  INDEX idx_assinatura_vendedor_status (vendedor_id,status),
  INDEX idx_assinatura_vencimento (status,vencimento_em)
);

CREATE TABLE evento_assinatura (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  assinatura_id BIGINT NOT NULL,
  ator_id BIGINT NOT NULL,
  tipo VARCHAR(40) NOT NULL,
  detalhe VARCHAR(500) NULL,
  criado_em DATETIME(6) NOT NULL,
  CONSTRAINT fk_evento_assinatura FOREIGN KEY (assinatura_id) REFERENCES assinatura_vendedor(id),
  CONSTRAINT fk_evento_assinatura_ator FOREIGN KEY (ator_id) REFERENCES usuario(id),
  INDEX idx_evento_assinatura_historico (assinatura_id,criado_em)
);

CREATE TABLE contrato_versao (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  versao VARCHAR(40) NOT NULL,
  titulo VARCHAR(160) NOT NULL,
  conteudo LONGTEXT NOT NULL,
  hash_sha256 CHAR(64) NOT NULL,
  publicado BOOLEAN NOT NULL DEFAULT FALSE,
  criado_em DATETIME(6) NOT NULL,
  CONSTRAINT uq_contrato_versao UNIQUE (versao),
  INDEX idx_contrato_publicado (publicado,criado_em)
);

INSERT INTO contrato_versao (versao,titulo,conteudo,hash_sha256,publicado,criado_em)
VALUES (
 '1.0-DEMO',
 'Minuta demonstrativa de contratação Auto Minas',
 'MINUTA DEMONSTRATIVA — PENDENTE DE REVISÃO JURÍDICA.\n\nCONTRATANTE: {{RAZAO_SOCIAL}}, CNPJ {{CNPJ}}, endereço {{ENDERECO}}. CONTRATANTE ANUNCIANTE: {{NOME_COMPLETO}}, CPF {{CPF_MASCARADO}}.\n\nPLANO E MENSALIDADE: {{PLANO}}, R$ {{MENSALIDADE}} por mês. A cobrança desta versão é externa e não está habilitada no Auto Minas. A assinatura só poderá ser ativada após confirmação real do pagamento.\n\nCOMISSÃO: {{COMISSAO_PERCENTUAL}}% sobre o valor final declarado de venda, sujeita à conferência e cobrança externas.\n\nRESPONSABILIDADES DO ANUNCIANTE: fornecer informações verdadeiras, manter documentação válida e informar a conclusão da venda.\n\nNEGOCIAÇÃO E PAGAMENTO DO VEÍCULO: comprador e anunciante negociam e realizam o pagamento diretamente, fora do Auto Minas. A plataforma não processa nem garante o pagamento do veículo.\n\nCANCELAMENTO: o anunciante poderá solicitar cancelamento; efeitos sobre vigência, mensalidade e comissões dependem da política comercial definitiva.\n\nRESPONSABILIDADES DA PLATAFORMA: disponibilizar ferramentas de divulgação e comunicação conforme disponibilidade e regras publicadas.\n\nTRATAMENTO DE DADOS: os dados serão tratados para cadastro, verificação, execução do serviço e cumprimento das obrigações aplicáveis, conforme aviso de privacidade.\n\nENCERRAMENTO: a plataforma poderá suspender ou encerrar o serviço nas hipóteses previstas nos termos definitivos e na legislação aplicável.\n\nEste documento não identifica a pessoa jurídica real nem constitui contrato comercial publicado. O aceite nesta versão é demonstrativo e não autoriza operação comercial.',
 SHA2('MINUTA DEMONSTRATIVA — PENDENTE DE REVISÃO JURÍDICA.\n\nCONTRATANTE: {{RAZAO_SOCIAL}}, CNPJ {{CNPJ}}, endereço {{ENDERECO}}. CONTRATANTE ANUNCIANTE: {{NOME_COMPLETO}}, CPF {{CPF_MASCARADO}}.\n\nPLANO E MENSALIDADE: {{PLANO}}, R$ {{MENSALIDADE}} por mês. A cobrança desta versão é externa e não está habilitada no Auto Minas. A assinatura só poderá ser ativada após confirmação real do pagamento.\n\nCOMISSÃO: {{COMISSAO_PERCENTUAL}}% sobre o valor final declarado de venda, sujeita à conferência e cobrança externas.\n\nRESPONSABILIDADES DO ANUNCIANTE: fornecer informações verdadeiras, manter documentação válida e informar a conclusão da venda.\n\nNEGOCIAÇÃO E PAGAMENTO DO VEÍCULO: comprador e anunciante negociam e realizam o pagamento diretamente, fora do Auto Minas. A plataforma não processa nem garante o pagamento do veículo.\n\nCANCELAMENTO: o anunciante poderá solicitar cancelamento; efeitos sobre vigência, mensalidade e comissões dependem da política comercial definitiva.\n\nRESPONSABILIDADES DA PLATAFORMA: disponibilizar ferramentas de divulgação e comunicação conforme disponibilidade e regras publicadas.\n\nTRATAMENTO DE DADOS: os dados serão tratados para cadastro, verificação, execução do serviço e cumprimento das obrigações aplicáveis, conforme aviso de privacidade.\n\nENCERRAMENTO: a plataforma poderá suspender ou encerrar o serviço nas hipóteses previstas nos termos definitivos e na legislação aplicável.\n\nEste documento não identifica a pessoa jurídica real nem constitui contrato comercial publicado. O aceite nesta versão é demonstrativo e não autoriza operação comercial.', 256),
 FALSE, UTC_TIMESTAMP(6)
);

CREATE TABLE aceite_contrato (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  contrato_id BIGINT NOT NULL,
  hash_aceito CHAR(64) NOT NULL,
  tipo_aceite VARCHAR(30) NOT NULL,
  aceito_em DATETIME(6) NOT NULL,
  provedor_assinatura VARCHAR(80) NULL,
  referencia_provedor VARCHAR(160) NULL,
  CONSTRAINT fk_aceite_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
  CONSTRAINT fk_aceite_contrato FOREIGN KEY (contrato_id) REFERENCES contrato_versao(id),
  CONSTRAINT uq_aceite_usuario_contrato UNIQUE (usuario_id,contrato_id),
  INDEX idx_aceite_usuario_data (usuario_id,aceito_em)
);

CREATE TABLE venda_declarada (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  anuncio_id BIGINT NOT NULL,
  vendedor_id BIGINT NOT NULL,
  assinatura_id BIGINT NOT NULL,
  plano_codigo_snapshot VARCHAR(30) NOT NULL,
  plano_nome_snapshot VARCHAR(80) NOT NULL,
  valor_final DECIMAL(15,2) NOT NULL,
  comissao_percentual DECIMAL(5,2) NOT NULL,
  comissao_calculada DECIMAL(15,2) NOT NULL,
  data_venda_declarada DATE NOT NULL,
  declarado_em DATETIME(6) NOT NULL,
  situacao VARCHAR(40) NOT NULL,
  CONSTRAINT fk_venda_anuncio FOREIGN KEY (anuncio_id) REFERENCES anuncio(id),
  CONSTRAINT fk_venda_vendedor FOREIGN KEY (vendedor_id) REFERENCES usuario(id),
  CONSTRAINT fk_venda_assinatura FOREIGN KEY (assinatura_id) REFERENCES assinatura_vendedor(id),
  CONSTRAINT uq_venda_anuncio UNIQUE (anuncio_id),
  CONSTRAINT chk_venda_valor CHECK (valor_final > 0),
  INDEX idx_venda_vendedor_data (vendedor_id,data_venda_declarada)
);
