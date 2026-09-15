package br.com.sprint1.challenge.service;

import br.com.sprint1.challenge.service.impl.SmtpPasswordResetEmailService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PasswordResetEmailServiceTest {

    @Test
    void sendPasswordResetEmail_incluiTokenEDestino() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        var service = new SmtpPasswordResetEmailService(
                mailSender,
                validMailProperties(),
                "no-reply@example.com");
        service.validateConfiguration();

        service.sendPasswordResetEmail("user@example.com", "reset-token");

        var messageCaptor = forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertEquals("no-reply@example.com", message.getFrom());
        assertEquals("user@example.com", message.getTo()[0]);
        assertTrue(message.getText().contains("reset-token"));
        assertTrue(message.getText().contains("15 minutos"));
    }

    @Test
    void validateConfiguration_rejeitaStartTlsDesabilitado() {
        MailProperties properties = validMailProperties();
        properties.getProperties().put("mail.smtp.starttls.required", "false");
        var service = new SmtpPasswordResetEmailService(
                mock(JavaMailSender.class),
                properties,
                "no-reply@example.com");

        assertThrows(IllegalStateException.class, service::validateConfiguration);
    }

    private MailProperties validMailProperties() {
        MailProperties properties = new MailProperties();
        properties.setHost("smtp.example.com");
        properties.setPort(587);
        properties.setUsername("smtp-user");
        properties.setPassword("smtp-password");
        properties.getProperties().put("mail.smtp.auth", "true");
        properties.getProperties().put("mail.smtp.starttls.enable", "true");
        properties.getProperties().put("mail.smtp.starttls.required", "true");
        return properties;
    }
}
