package com.josenetoo_dev.veiculos_api.security;

import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class JwtUtil {
    private final SecretKey key;
    private final UsuarioRepository usuarios;
    private final long expirationMs;

    public JwtUtil(@Value("${jwt.secret:}") String secret,
                   @Value("${jwt.expiration-ms:1800000}") long expirationMs,
                   Environment environment, UsuarioRepository usuarios) {
        boolean local = Arrays.asList(environment.getActiveProfiles()).contains("local")
                && !Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (secret.isBlank() && local) {
            key = Keys.secretKeyFor(SignatureAlgorithm.HS256); // efêmera, não versionada
        } else {
            if (secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 32
                    || secret.contains("AutoMinasSecretKey")) {
                throw new IllegalStateException("JWT_SECRET deve ser fornecido com pelo menos 32 bytes e sem valor padrão conhecido");
            }
            key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        if (expirationMs < 60000 || expirationMs > 1800000) {
            throw new IllegalStateException("Expiração JWT deve estar entre 1 e 30 minutos");
        }
        this.expirationMs = expirationMs;
        this.usuarios = usuarios;
    }

    public String gerarToken(String subjectId) {
        var usuario = usuarios.findById(Long.valueOf(subjectId)).orElseThrow();
        if (!usuario.getStatus().podeAutenticar()) throw new IllegalStateException("Conta indisponível");
        return Jwts.builder().setSubject(subjectId).claim("tv", usuario.getTokenVersion())
                .setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256).compact();
    }
    public Claims extrairClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }
    public String extrairSubject(String token) { return extrairClaims(token).getSubject(); }
    public boolean validarToken(String token) {
        try { extrairClaims(token); return true; } catch (JwtException | IllegalArgumentException e) { return false; }
    }
}
