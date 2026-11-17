CREATE TABLE recommendation_signal (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 session_key CHAR(36) NULL,
 user_id BIGINT NULL,
 event_id CHAR(36) NOT NULL,
 kind VARCHAR(24) NOT NULL,
 target_key VARCHAR(128) NOT NULL,
 event_day DATE NOT NULL,
 created_at DATETIME(6) NOT NULL,
 strength DOUBLE NOT NULL,
 brand VARCHAR(255), model_name VARCHAR(255), category VARCHAR(64),
 price_band BIGINT, region CHAR(2),
 FOREIGN KEY (user_id) REFERENCES usuario(id) ON DELETE CASCADE,
 UNIQUE (session_key,event_id), UNIQUE (user_id,event_id),
 UNIQUE (session_key,kind,target_key,event_day), UNIQUE (user_id,kind,target_key,event_day),
 INDEX ix_rec_session (session_key,created_at), INDEX ix_rec_user (user_id,created_at),
 INDEX ix_rec_retention (created_at)
);
-- Concessão explícita por contratação. Nenhum plano legado recebe mídia automaticamente.
CREATE TABLE recommendation_home_benefit (
 subscription_id BIGINT PRIMARY KEY,
 FOREIGN KEY (subscription_id) REFERENCES assinatura_vendedor(id) ON DELETE CASCADE
);
CREATE INDEX ix_rec_inventory ON anuncio(status,criado_em,id);
