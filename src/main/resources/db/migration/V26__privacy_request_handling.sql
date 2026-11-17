ALTER TABLE solicitacao_privacidade
 ADD COLUMN resposta_publica VARCHAR(2000) NULL,
 ADD COLUMN respondido_em DATETIME(6) NULL,
 ADD COLUMN responsavel_id BIGINT NULL,
 ADD COLUMN historico_atendimento LONGTEXT NULL;
