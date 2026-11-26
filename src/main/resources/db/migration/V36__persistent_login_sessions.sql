CREATE TABLE login_session (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    token_version BIGINT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_login_session_token_hash (token_hash),
    KEY ix_login_session_user (usuario_id),
    KEY ix_login_session_expires (expires_at),
    CONSTRAINT fk_login_session_user FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);
