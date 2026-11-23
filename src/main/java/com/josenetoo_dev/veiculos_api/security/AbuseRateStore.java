package com.josenetoo_dev.veiculos_api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

/** Exact sliding windows, serialized per key by a database row lock across application instances. */
@Service
public class AbuseRateStore {
    private final JdbcTemplate db;
    private final TransactionTemplate transactions;
    private final byte[] secret;

    public AbuseRateStore(JdbcTemplate db, TransactionTemplate transactions,
            @Value("${jwt.secret}") String secret) {
        this.db = db;
        this.transactions = transactions;
        if (secret == null || secret.length() < 32) throw new IllegalStateException("JWT_SECRET insuficiente para proteção antiabuso");
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public long consume(String identity, int limit, int seconds) {
        if (limit < 1 || seconds < 1) throw new IllegalArgumentException("Limite inválido");
        String key = hash(identity);
        return transactions.execute(status -> {
            Instant now = Instant.now();
            db.update("INSERT IGNORE INTO abuse_rate_key (key_hash,last_seen) VALUES (?,?)", key, now);
            db.queryForObject("SELECT last_seen FROM abuse_rate_key WHERE key_hash=? FOR UPDATE",
                    (rs, row) -> rs.getTimestamp(1), key);
            Instant cutoff = now.minusSeconds(seconds);
            // One key is locked, so simultaneous requests on other nodes cannot pass the same quota.
            Integer count = db.queryForObject("SELECT COUNT(*) FROM abuse_rate_event WHERE key_hash=? AND occurred_at>?",
                    Integer.class, key, cutoff);
            if (count != null && count >= limit) {
                Instant oldest = db.queryForObject("SELECT MIN(occurred_at) FROM (SELECT occurred_at FROM abuse_rate_event WHERE key_hash=? AND occurred_at>? ORDER BY occurred_at LIMIT ?) recent",
                        (rs, row) -> rs.getTimestamp(1).toInstant(), key, cutoff, limit);
                return Math.max(1, oldest.plusSeconds(seconds).getEpochSecond() - now.getEpochSecond());
            }
            db.update("INSERT INTO abuse_rate_event (key_hash,occurred_at) VALUES (?,?)", key, now);
            db.update("UPDATE abuse_rate_key SET last_seen=? WHERE key_hash=?", now, key);
            return 0L;
        });
    }

    @Scheduled(fixedDelayString = "${app.abuse.cleanup-ms:3600000}")
    public void cleanup() {
        // Longest configured window must be <= 24 hours. Bounded batches avoid long transactions.
        db.update("DELETE FROM abuse_rate_event WHERE occurred_at < ? LIMIT 10000", Instant.now().minusSeconds(86400));
        db.update("DELETE k FROM abuse_rate_key k LEFT JOIN abuse_rate_event e ON e.key_hash=k.key_hash "
                + "WHERE e.id IS NULL AND k.last_seen < ? LIMIT 10000", Instant.now().minusSeconds(86400));
    }

    private String hash(String identity) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(identity.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Falha na proteção antiabuso", e);
        }
    }
}
