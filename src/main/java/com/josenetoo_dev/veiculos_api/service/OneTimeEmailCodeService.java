package com.josenetoo_dev.veiculos_api.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OTP de 6 dígitos. Exclusividade entre TODOS os desafios ativos garantida
 * por índice UNIQUE no MySQL, inclusive entre processos concorrentes.
 * SHA-256 legado permanece verificável até vencer o prazo de 15 minutos.
 *
 * Nunca registrar códigos ou devolver códigos na API.
 */
@Service
public class OneTimeEmailCodeService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;
    private final byte[] macKey;

    public OneTimeEmailCodeService(JdbcTemplate jdbc, @Value("${jwt.secret:}") String jwtSecret) {
        this.jdbc = jdbc;
        byte[] raw;
        if (jwtSecret.isBlank()) {
            // Apenas ambientes locais/de teste sem JWT_SECRET configurado.
            // A API de produção já exige JWT_SECRET de >=32 bytes no JwtUtil.
            raw = new byte[32];
            RANDOM.nextBytes(raw);
        } else {
            raw = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update("auto-minas:email-otp:hmac:v1:".getBytes(StandardCharsets.UTF_8));
            this.macKey = digest.digest(raw);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao inicializar gerador de códigos",ex);
        }
    }

    /** Deve ser chamado de dentro da transação de emissão (usuário bloqueado). */
    public String reserve() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (int i = 0; i < 64; i++) {
            String candidate = String.format(java.util.Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
            try {
                jdbc.update(
                    "INSERT INTO email_code_reservation (code_hash, expires_at, created_at) VALUES (?, ?, ?)",
                    hash(candidate),now.plusMinutes(15),now);
                return candidate;
            } catch (DuplicateKeyException collision) {
                // Outro desafio vigente já usa este código: tentar novo valor.
            }
        }
        throw new IllegalStateException("Não foi possível reservar código exclusivo");
    }

    /** HMAC para novos códigos; SHA-256 apenas para códigos legados longos. */
    public String hash(String code) {
        if (!code.matches("\\d{6}")) return legacySha256(code);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(macKey,"HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(code.getBytes(StandardCharsets.US_ASCII)));
        } catch (Exception ex) {
            throw new IllegalStateException("Algoritmo HMAC indisponível",ex);
        }
    }

    private String legacySha256(String code) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 indisponível",ex);
        }
    }

    @Scheduled(fixedDelay = 900_000L)
    @Transactional
    public void purgeExpired() {
        jdbc.update("DELETE FROM email_code_reservation WHERE expires_at <= ?",
            LocalDateTime.now(ZoneOffset.UTC));
    }
}
