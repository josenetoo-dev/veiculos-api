-- Reservas globais de OTP: impede colisão entre códigos válidos de usuários
-- diferentes, inclusive com múltiplas instâncias Spring concorrentes.
-- Guarda apenas HMAC irreversível; expira automaticamente na emissão seguinte
-- e também é limpo em rotina agendada. Nenhum código em texto puro no banco.
CREATE TABLE email_code_reservation (
  code_hash VARCHAR(64) NOT NULL PRIMARY KEY,
  expires_at DATETIME(6) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  INDEX idx_email_code_reservation_expiry (expires_at)
);
