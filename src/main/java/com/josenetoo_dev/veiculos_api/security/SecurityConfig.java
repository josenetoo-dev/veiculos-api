package com.josenetoo_dev.veiculos_api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Preflight OPTIONS sempre liberado — obrigatório para CORS com Authorization header
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers(
                                "/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/error"
                        ).permitAll()

                        .requestMatchers("/v1/admin/**", "/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/v1/usuario", "/v1/usuario/buscar")
                            .hasAnyRole("ADMIN", "REVIEWER")
                        .requestMatchers(HttpMethod.GET, "/v1/anuncio", "/v1/anuncio/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                SecurityErrors.write(response, 401, "Não autenticado", "AUTHENTICATION_REQUIRED"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                SecurityErrors.write(response, 403, "Acesso negado", "ACCESS_DENIED"))
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        String frontendUrl = System.getenv("FRONTEND_URL");
        List<String> origens = new ArrayList<>(List.of(
                "http://localhost:5500",
                "http://127.0.0.1:5500",
                "http://localhost:3000",
                "http://127.0.0.1:3000"
        ));
        if (frontendUrl != null && !frontendUrl.isBlank()) {
            origens.add(frontendUrl);
        }
        config.setAllowedOrigins(origens);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Lista explícita — allowedHeaders("*") com allowCredentials(true) não funciona no Spring
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With"
        ));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}