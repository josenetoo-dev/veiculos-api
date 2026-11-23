-- Chaves são hashes com segredo da aplicação; IPs e identificadores não são persistidos em claro.
CREATE TABLE abuse_rate_key (
    key_hash CHAR(64) NOT NULL PRIMARY KEY,
    last_seen TIMESTAMP(6) NOT NULL,
    INDEX ix_abuse_key_last_seen (last_seen)
);

CREATE TABLE abuse_rate_event (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    key_hash CHAR(64) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    INDEX ix_abuse_event_window (key_hash, occurred_at),
    INDEX ix_abuse_event_cleanup (occurred_at),
    CONSTRAINT fk_abuse_rate_key FOREIGN KEY (key_hash) REFERENCES abuse_rate_key(key_hash)
);
