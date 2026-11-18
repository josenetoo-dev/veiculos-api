-- Declarações novas ficam nulas nos anúncios antigos, sem inventar respostas.
-- A aplicação valida as sete respostas obrigatórias ao publicar novos anúncios.
ALTER TABLE anuncio ADD COLUMN historico_colisao VARCHAR(20) NULL;
ALTER TABLE anuncio ADD COLUMN historico_detalhes_colisao VARCHAR(500) NULL;
ALTER TABLE anuncio ADD COLUMN historico_repintura VARCHAR(20) NULL;
ALTER TABLE anuncio ADD COLUMN historico_detalhes_repintura VARCHAR(500) NULL;
ALTER TABLE anuncio ADD COLUMN historico_pecas_acidente VARCHAR(20) NULL;
ALTER TABLE anuncio ADD COLUMN historico_detalhes_pecas_acidente VARCHAR(500) NULL;
ALTER TABLE anuncio ADD COLUMN historico_leilao VARCHAR(20) NULL;
ALTER TABLE anuncio ADD COLUMN historico_detalhes_leilao VARCHAR(500) NULL;
ALTER TABLE anuncio ADD COLUMN historico_alagamento VARCHAR(20) NULL;
ALTER TABLE anuncio ADD COLUMN historico_detalhes_alagamento VARCHAR(500) NULL;
ALTER TABLE anuncio ADD COLUMN historico_problema_mecanico VARCHAR(20) NULL;
ALTER TABLE anuncio ADD COLUMN historico_detalhes_problema_mecanico VARCHAR(500) NULL;
ALTER TABLE anuncio ADD COLUMN historico_sinistro VARCHAR(20) NULL;
ALTER TABLE anuncio ADD COLUMN historico_detalhes_sinistro VARCHAR(500) NULL;
