package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.exception.ex.EmailChangeUnavailableException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class ContatoEmailMailer {
    private final ObjectProvider<JavaMailSender> senders;
    private final boolean enabled;
    private final String from;
    private final String host;

    public ContatoEmailMailer(
            ObjectProvider<JavaMailSender> senders,
            @Value("${app.contact-email.enabled:false}") boolean enabled,
            @Value("${app.contact-email.from:}") String from,
            @Value("${spring.mail.host:}") String host) {
        this.senders = senders;
        this.enabled = enabled;
        this.from = from;
        this.host = host;
    }

    public boolean available() {
        return enabled && !from.isBlank() && !host.isBlank() && senders.getIfAvailable() != null;
    }

    public void send(String email, String token) {
        if (!available()) throw new EmailChangeUnavailableException();
        try {
            var sender = senders.getObject();
            var message = sender.createMimeMessage();
            var mail = new MimeMessageHelper(message, true, "UTF-8");
            mail.setFrom(from);
            mail.setTo(email);
            mail.setSubject("Auto Minas — confirme seu e-mail");
            String text = "Confirme seu e-mail no Auto Minas. Cole este código na sua conta:\n\n"
                    + token + "\n\nVálido por 15 minutos. Se não solicitou, ignore esta mensagem.";
            String html = """
                    <!doctype html><html lang="pt-BR"><meta name="viewport" content="width=device-width,initial-scale=1">
                    <body style="margin:0;background:#101114;color:#eee5d3;font-family:Arial,sans-serif">
                    <div style="max-width:560px;margin:auto;padding:24px">
                    <h1 style="font-size:24px;color:#e4ca91">Auto Minas</h1>
                    <h2 style="font-size:20px">Confirme seu e-mail</h2>
                    <p>Cole o código abaixo na área Minha conta.</p>
                    <p style="background:#24241f;border:1px solid #b89534;padding:16px;overflow-wrap:anywhere">%s</p>
                    <p>O código expira em 15 minutos e só pode ser usado uma vez.</p>
                    <p>Se você não solicitou esta confirmação, ignore esta mensagem.</p>
                    </div></body></html>
                    """.formatted(token);
            mail.setText(text, html);
            sender.send(message);
        } catch (MailException | jakarta.mail.MessagingException ex) {
            throw new EmailChangeUnavailableException();
        }
    }
}
