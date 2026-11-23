-- Only offers for NEW contracts change. Historical plans, subscriptions and snapshots remain intact.
ALTER TABLE plano_comercial MODIFY COLUMN mensalidade DECIMAL(10,2) NULL;
UPDATE plano_comercial SET ativo=FALSE WHERE codigo <> 'AUTO_MINAS_PRO';
INSERT INTO plano_comercial (codigo,nome,mensalidade,comissao_percentual,beneficios_json,
  ativo,criado_em,atualizado_em,periodicidade,limite_anuncios,vigencia_dias)
VALUES ('AUTO_MINAS_PRO','Auto Minas PRO',NULL,0,
  '["Exposição patrocinada sujeita a elegibilidade","Gestão de anúncios e negociações"]',
  TRUE,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),'MENSAL',2147483647,30)
ON DUPLICATE KEY UPDATE ativo=TRUE;
