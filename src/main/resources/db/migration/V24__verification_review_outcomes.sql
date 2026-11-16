-- Classifica decisões sem reescrever históricos nem inventar verificações anteriores.
ALTER TABLE verificacao ADD COLUMN categoria_resultado VARCHAR(40) NULL;
ALTER TABLE evento_verificacao ADD COLUMN categoria_resultado VARCHAR(40) NULL;
