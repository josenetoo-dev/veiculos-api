package com.josenetoo_dev.veiculos_api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Uses the socket peer address. Forwarded headers are ignored until a trusted proxy is configured. */
@Component
public class AbuseRateFilter extends OncePerRequestFilter {
    private final AbuseRateStore store;
    private final boolean enabled;
    private final int authLimit;
    private final int writeLimit;
    private final int eventsLimit;

    public AbuseRateFilter(AbuseRateStore store,
            @Value("${app.abuse.enabled:true}") boolean enabled,
            @Value("${app.abuse.auth-per-minute:80}") int authLimit,
            @Value("${app.abuse.write-per-minute:120}") int writeLimit,
            @Value("${app.abuse.events-per-minute:120}") int eventsLimit) {
        this.store = store;
        this.enabled = enabled;
        this.authLimit = authLimit;
        this.writeLimit = writeLimit;
        this.eventsLimit = eventsLimit;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!enabled || "OPTIONS".equals(request.getMethod())) { chain.doFilter(request, response); return; }
        String path = request.getRequestURI();
        if (!"POST".equals(request.getMethod()) && !"PUT".equals(request.getMethod())
                && !"PATCH".equals(request.getMethod())) { chain.doFilter(request, response); return; }
        String group;
        int limit;
        if (path.startsWith("/auth/") || path.contains("email") || path.contains("senha")) {
            group = "auth"; limit = authLimit;
        } else if (path.equals("/v1/recommendations/events")) {
            group = "events"; limit = eventsLimit;
        } else if (path.contains("mensagens") || path.contains("denuncias") || path.contains("upload")
                || path.contains("evidencias") || path.contains("anuncio") || path.contains("propostas")) {
            group = "writes"; limit = writeLimit;
        } else { chain.doFilter(request, response); return; }

        // A generous shared-IP ceiling accommodates families, CGNAT and school networks.
        // A separate account quota limits a single signed-in sender on the same connection.
        long retry = store.consume("ip:" + group + ":" + request.getRemoteAddr(), limit, 60);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (retry == 0 && auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            retry = store.consume("user:" + group + ":" + auth.getName(), Math.max(10, limit / 3), 60);
        }
        if (retry > 0) {
            response.setHeader("Retry-After", Long.toString(retry));
            SecurityErrors.write(response, 429, "Muitas solicitações", "RATE_LIMITED");
            return;
        }
        chain.doFilter(request, response);
    }
}
