package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.exception.ex.EmailChangeUnavailableException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * O código de confirmação é enviado apenas ao novo endereço e nunca exposto por API ou logs.
 * O recurso permanece desabilitado até configurar SMTP e remetente na infraestrutura.
 */
@Component
public class EmailChangeMailer {
    private final ObjectProvider<JavaMailSender> senders;
    private final boolean enabled;
    private final String from;
    private final String host;

    public EmailChangeMailer(ObjectProvider<JavaMailSender> senders,
            @Value("${app.email-change.enabled:false}") boolean enabled,
            @Value("${app.email-change.from:}") String from,
            @Value("${spring.mail.host:}") String host) {
        this.senders = senders;
        this.enabled = enabled;
        this.from = from;
        this.host = host;
    }

    public boolean isConfigured() {
        return enabled && !from.isBlank() && !host.isBlank() && senders.getIfAvailable() != null;
    }

    public void sendConfirmation(String targetEmail, String code) {
        if (!isConfigured()) throw new EmailChangeUnavailableException();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(targetEmail);
        message.setSubject("Auto Minas — confirmação de alteração de e-mail");
        message.setText("Você solicitou alterar o e-mail da sua conta Auto Minas.\n\n"
                + "Código: " + code + "\n\n"
                + "Este código expira em 15 minutos. Se não foi você, ignore esta mensagem.\n");
        try {
            senders.getObject().send(message);
        } catch (MailException e) {
            throw new EmailChangeUnavailableException();
        }
    }
}
