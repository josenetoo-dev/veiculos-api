package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.exception.ex.EmailChangeUnavailableException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** SMTP optional: never send or reveal a reset token without a configured sender. */
@Component
public class PasswordResetMailer {
    private final ObjectProvider<JavaMailSender> senders;
    private final boolean enabled;
    private final String from;
    private final String host;

    public PasswordResetMailer(
        ObjectProvider<JavaMailSender> senders,
        @Value("${app.password-reset.enabled:false}") boolean enabled,
        @Value("${app.password-reset.from:}") String from,
        @Value("${spring.mail.host:}") String host) {
        this.senders=senders;this.enabled=enabled;this.from=from;this.host=host;
    }

    public boolean configured() {
        return enabled && !from.isBlank() && !host.isBlank() && senders.getIfAvailable()!=null;
    }

    public void send(String recipient, String token) {
        if (!configured()) throw new EmailChangeUnavailableException();
        SimpleMailMessage message=new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Auto Minas — recuperação de senha");
        message.setText("Você solicitou redefinir a senha da sua conta Auto Minas.\n\n"
            + "Código: "+token+"\n\n"
            + "Ele expira em 15 minutos e pode ser usado apenas uma vez. "
            + "Se não fez o pedido, ignore este e-mail.\n");
        try { senders.getObject().send(message); }
        catch (MailException ex) { throw new EmailChangeUnavailableException(); }
    }
}
