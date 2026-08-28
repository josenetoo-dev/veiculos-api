package com.josenetoo_dev.veiculos_api.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private final long expiracaoMs = 86400000;

    private Key getChave() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Usa o id, não o e-mail: o e-mail pode ser trocado depois (PUT /v1/usuario/{id}),
    // e um token com e-mail antigo como subject pararia de bater com o usuário no banco,
    // derrubando a sessão em toda ação seguinte até fazer login de novo.
    public String gerarToken(String subjectId) {
        return Jwts.builder()
                .setSubject(subjectId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiracaoMs))
                .signWith(getChave(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extrairSubject(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getChave())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validarToken(String token) {
        try {
            extrairSubject(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}