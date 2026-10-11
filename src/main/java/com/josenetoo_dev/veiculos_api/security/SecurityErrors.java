package com.josenetoo_dev.veiculos_api.security;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
public final class SecurityErrors {
    private SecurityErrors() {}
    public static void write(HttpServletResponse response, int status, String title, String code) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        // Todos os argumentos são constantes internas; nenhum dado de requisição ou exceção entra no JSON.
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status
                + ",\"detail\":\"" + title + "\",\"code\":\"" + code + "\"}");
    }
}
