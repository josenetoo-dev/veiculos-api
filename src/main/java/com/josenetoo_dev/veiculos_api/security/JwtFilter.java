package com.josenetoo_dev.veiculos_api.security;

import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarios;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                var claims = jwtUtil.extrairClaims(header.substring(7));
                Long id = Long.valueOf(claims.getSubject());
                Number version = claims.get("tv", Number.class);
                var usuario = usuarios.findById(id).orElse(null);
                if (usuario == null || version == null || version.longValue() != usuario.getTokenVersion()
                        || !usuario.getStatus().podeAutenticar()) {
                    SecurityContextHolder.clearContext();
                    SecurityErrors.write(response, 401, "Não autenticado", "AUTHENTICATION_REQUIRED");
                    return;
                }
                var auth = new UsernamePasswordAuthenticationToken(id.toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
                SecurityErrors.write(response, 401, "Não autenticado", "AUTHENTICATION_REQUIRED");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
