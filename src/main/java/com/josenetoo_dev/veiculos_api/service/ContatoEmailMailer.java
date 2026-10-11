package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.exception.ex.EmailChangeUnavailableException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
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
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(email);
        mail.setSubject("Auto Minas — confirme seu e-mail");
        mail.setText("Para confirmar o e-mail da sua conta Auto Minas, utilize este código no aplicativo:\n\n"
                + token + "\n\nO código expira em 15 minutos. Se você não criou a conta, ignore.\n");
        try {
            senders.getObject().send(mail);
        } catch (MailException ex) {
            throw new EmailChangeUnavailableException();
        }
    }
}
