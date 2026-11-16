package com.josenetoo_dev.veiculos_api.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BrevoMailSenderTest {
    private HttpClient client;
    private HttpResponse<String> response;
    private BrevoMailSender sender;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() throws Exception {
        client = mock(HttpClient.class);
        response = mock(HttpResponse.class);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        when(response.statusCode()).thenReturn(201);
        when(response.body()).thenReturn("{\"messageId\":\"test-accepted\"}");
        sender = new BrevoMailSender(client, new ObjectMapper(), "fictitious-api-key");
    }

    private SimpleMailMessage plain() {
        var message = new SimpleMailMessage();
        message.setFrom("sender@example.com");
        message.setTo("test@example.com");
        message.setSubject("Recuperação");
        message.setText("Código fictício");
        return message;
    }

    @Test
    void plainMailUsesHttpsAndApiKey() throws Exception {
        sender.send(plain());
        var captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(captor.capture(), any());
        var request = captor.getValue();
        assertEquals("https://api.brevo.com/v3/smtp/email", request.uri().toString());
        assertEquals("fictitious-api-key", request.headers().firstValue("api-key").orElseThrow());
        assertEquals("POST", request.method());
    }

    @Test
    void multipartVerificationIsAccepted() throws Exception {
        var message = sender.createMimeMessage();
        var helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom("sender@example.com");
        helper.setTo("test@example.com");
        helper.setSubject("Confirmação");
        helper.setText("Código fictício", "<html><body>Código fictício</body></html>");
        sender.send(message);
        var captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client, times(1)).send(captor.capture(), any());
        var subscriber = HttpResponse.BodySubscribers.ofString(java.nio.charset.StandardCharsets.UTF_8);
        captor.getValue().bodyPublisher().orElseThrow().subscribe(subscriber);
        var payload = new ObjectMapper().readTree(subscriber.getBody().toCompletableFuture().get());
        assertEquals("sender@example.com", payload.get("sender").get("email").asText());
        assertEquals("test@example.com", payload.get("to").get(0).get("email").asText());
        assertEquals("Código fictício", payload.get("textContent").asText());
        assertTrue(payload.get("htmlContent").asText().contains("Código fictício"));
    }

    @Test
    void providerErrorsAreSanitizedAndNeverRetried() throws Exception {
        for (int status : new int[]{400, 401, 403, 429, 500}) {
            when(response.statusCode()).thenReturn(status);
            when(response.body()).thenReturn("secret-provider-payload");
            var error = assertThrows(MailSendException.class, () -> sender.send(plain()));
            assertFalse(error.toString().contains("secret-provider-payload"));
            assertFalse(error.toString().contains("fictitious-api-key"));
        }
        verify(client, times(5)).send(any(), any());
    }

    @Test
    void acceptanceRequiresMessageIdentifier() {
        when(response.body()).thenReturn("{}");
        assertThrows(MailSendException.class, () -> sender.send(plain()));
    }

    @Test
    void networkFailureDoesNotExposeSecrets() throws Exception {
        when(client.send(any(), any())).thenThrow(new java.io.IOException("secret-provider-payload"));
        var error = assertThrows(MailSendException.class, () -> sender.send(plain()));
        assertFalse(error.toString().contains("secret-provider-payload"));
        verify(client, times(1)).send(any(), any());
    }
}
