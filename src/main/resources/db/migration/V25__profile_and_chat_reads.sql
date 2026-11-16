ALTER TABLE usuario ADD COLUMN foto_perfil_chave VARCHAR(36), ADD COLUMN foto_perfil_tipo VARCHAR(20);
ALTER TABLE anuncio ADD COLUMN nivel_um_revisado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE mensagem ADD COLUMN lida_em DATETIME(6);
CREATE INDEX idx_mensagem_pendencia ON mensagem(proposta_id, lida_em, excluida_em, remetente_id);
