package com.josenetoo_dev.veiculos_api.mail;

import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Preserves existing JavaMailSender callers while transporting messages over HTTPS. */
public class BrevoMailSender extends JavaMailSenderImpl {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BrevoMailSender.class);
    private static final URI ENDPOINT = URI.create("https://api.brevo.com/v3/smtp/email");
    private final HttpClient client;
    private final ObjectMapper json;
    private final String apiKey;

    public BrevoMailSender(HttpClient client, ObjectMapper json, String apiKey) {
        this.client = client;
        this.json = json;
        this.apiKey = apiKey;
    }

    @Override
    public void send(SimpleMailMessage message) {
        if (message.getTo() == null || message.getFrom() == null) throw unavailable();
        String text = message.getText() == null ? "" : message.getText();
        transmit(message.getFrom(), Arrays.asList(message.getTo()), message.getSubject(), text,
                "<html><body><div style=\"font-family:Arial,sans-serif;max-width:560px;margin:auto;padding:24px\">"
                + HtmlUtils.htmlEscape(text).replace("\n", "<br>") + "</div></body></html>");
    }

    @Override
    public void send(SimpleMailMessage... messages) {
        for (var message : messages) send(message);
    }

    @Override
    public void send(MimeMessage message) {
        try {
            message.saveChanges();
            if (message.getFrom() == null || message.getFrom().length != 1
                    || message.getRecipients(Message.RecipientType.TO) == null) throw unavailable();
            Map<String, String> content = new LinkedHashMap<>();
            collect(message, content);
            String text = content.getOrDefault("text", "");
            String html = content.getOrDefault("html", "<html><body>" + HtmlUtils.htmlEscape(text) + "</body></html>");
            transmit(((InternetAddress) message.getFrom()[0]).getAddress(),
                    Arrays.stream(message.getRecipients(Message.RecipientType.TO))
                            .map(address -> ((InternetAddress) address).getAddress()).toList(),
                    message.getSubject(), text, html);
        } catch (Exception failure) {
            throw unavailable();
        }
    }

    @Override
    public void send(MimeMessage... messages) {
        for (var message : messages) send(message);
    }

    private void collect(Part part, Map<String, String> content) throws Exception {
        if (part.isMimeType("text/plain")) content.put("text", (String) part.getContent());
        else if (part.isMimeType("text/html")) content.put("html", (String) part.getContent());
        else if (part.getContent() instanceof Multipart multipart) {
            for (int index = 0; index < multipart.getCount(); index++) collect(multipart.getBodyPart(index), content);
        }
    }

    private void transmit(String from, List<String> recipients, String subject, String text, String html) {
        if (apiKey.isBlank() || recipients.isEmpty()) throw unavailable();
        try {
            var payload = Map.of(
                    "sender", Map.of("name", "Auto Minas", "email", from),
                    "to", recipients.stream().map(email -> Map.of("email", email)).toList(),
                    "subject", subject,
                    "textContent", text,
                    "htmlContent", html);
            var request = HttpRequest.newBuilder(ENDPOINT).timeout(Duration.ofSeconds(8))
                    .header("api-key", apiKey).header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload))).build();
            // No automatic retry: an ambiguous timeout may already have sent the message.
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201) {
                log.warn("Brevo rejeitou envio transacional: HTTP {}", response.statusCode());
                throw unavailable();
            }
            if (!json.readTree(response.body()).hasNonNull("messageId")) throw unavailable();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (Exception failure) {
            // Provider payloads may contain secrets or personal data. Never propagate them.
            throw unavailable();
        }
    }

    private MailSendException unavailable() {
        return new MailSendException("Envio transacional temporariamente indisponível");
    }
}
