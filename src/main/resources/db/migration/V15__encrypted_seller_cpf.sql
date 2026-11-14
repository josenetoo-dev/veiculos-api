ALTER TABLE usuario
  ADD COLUMN cpf_ciphertext LONGTEXT NULL,
  ADD COLUMN cpf_lookup_hash CHAR(64) NULL,
  ADD COLUMN cpf_final_digitos CHAR(2) NULL;

CREATE UNIQUE INDEX uq_usuario_cpf_lookup_hash ON usuario(cpf_lookup_hash);
