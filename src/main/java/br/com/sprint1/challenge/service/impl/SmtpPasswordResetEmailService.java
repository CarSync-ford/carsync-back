package br.com.sprint1.challenge.service.impl;

import br.com.sprint1.challenge.service.PasswordResetEmailService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpPasswordResetEmailService implements PasswordResetEmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final String from;

    public SmtpPasswordResetEmailService(
            JavaMailSender mailSender,
            MailProperties mailProperties,
            @Value("${mail.from}") String from) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.from = from;
    }

    @PostConstruct
    public void validateConfiguration() {
        if (isBlank(mailProperties.getHost())
                || mailProperties.getPort() == null
                || isBlank(mailProperties.getUsername())
                || isBlank(mailProperties.getPassword())
                || isBlank(from)) {
            throw new IllegalStateException("SMTP password reset configuration is incomplete");
        }
        if (!Boolean.parseBoolean(mailProperties.getProperties().get("mail.smtp.auth"))
                || !Boolean.parseBoolean(mailProperties.getProperties().get("mail.smtp.starttls.enable"))
                || !Boolean.parseBoolean(mailProperties.getProperties().get("mail.smtp.starttls.required"))) {
            throw new IllegalStateException("SMTP authentication and STARTTLS are required");
        }
    }

    @Override
    public void sendPasswordResetEmail(String recipient, String resetToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Redefinição de senha");
        message.setText("Use o token abaixo em /api/v1/auth/reset-password. "
                + "Ele expira em 15 minutos e só pode ser usado uma vez.\n\n"
                + resetToken);
        mailSender.send(message);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
