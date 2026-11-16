ALTER TABLE plano_comercial
 ADD COLUMN periodicidade VARCHAR(10) NOT NULL DEFAULT 'MENSAL',
 ADD COLUMN limite_anuncios INT NOT NULL DEFAULT 2147483647,
 ADD COLUMN vigencia_dias INT NOT NULL DEFAULT 30;
ALTER TABLE assinatura_vendedor
 ADD COLUMN periodicidade_snapshot VARCHAR(10) NOT NULL DEFAULT 'MENSAL',
 ADD COLUMN limite_anuncios_snapshot INT NOT NULL DEFAULT 2147483647,
 ADD COLUMN vigencia_dias_snapshot INT NOT NULL DEFAULT 30,
 ADD COLUMN renovacao_cancelada BOOLEAN NOT NULL DEFAULT FALSE;
-- Preserve legacy plan records, subscriptions, accepted contracts and snapshots.
UPDATE plano_comercial SET ativo=FALSE WHERE codigo IN ('BASIC','PROFESSIONAL','PREMIUM');
INSERT INTO plano_comercial (codigo,nome,mensalidade,comissao_percentual,beneficios_json,ativo,criado_em,atualizado_em,periodicidade,limite_anuncios,vigencia_dias)
VALUES
 ('AVULSO','Anúncio avulso',79.90,0,'["Um anúncio ativo","Vigência de 30 dias","Fotos e edição do anúncio","Propostas e mensagens","Renovação por nova contratação"]',TRUE,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),'UNICO',1,30),
 ('RECORRENTE','Assinatura mensal',69.90,0,'["Até três anúncios ativos simultaneamente","Vigência mensal","Gerenciamento de anúncios","Propostas e mensagens","Cancelamento da renovação preserva o período pago"]',TRUE,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),'MENSAL',3,30);
