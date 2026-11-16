package com.josenetoo_dev.veiculos_api.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "brevo")
public class BrevoMailConfiguration {
    @Bean
    public BrevoMailSender brevoMailSender(ObjectMapper json, @Value("${brevo.api-key:}") String key) {
        if (key.isBlank()) throw new IllegalStateException("BREVO_API_KEY obrigatória para o provedor Brevo");
        return new BrevoMailSender(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build(), json, key);
    }
}
