package org.sasanlabs.service.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class EmailServiceImplTest {

    private static final String FROM = "no-reply@vulnerableapp.local";
    private static final String BASE_URL = "http://localhost";

    @Mock private JavaMailSender javaMailSender;

    private EmailServiceImpl emailService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        EmailConfiguration emailConfiguration = new EmailConfiguration();
        emailConfiguration.setFrom(FROM);
        emailConfiguration.setBaseUrl(BASE_URL);
        emailService = new EmailServiceImpl(javaMailSender, emailConfiguration);
    }

    @Test
    void shouldSendPlainTextEmail() {
        emailService.sendEmail("student@example.com", "Subject", "Body");

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertEquals(FROM, message.getFrom());
        assertEquals("student@example.com", message.getTo()[0]);
        assertEquals("Subject", message.getSubject());
        assertEquals("Body", message.getText());
    }

    @Test
    void shouldSendHtmlEmail() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendHtmlEmail("student@example.com", "Subject", "<b>Body</b>");

        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void shouldSendResetEmailWithTokenLink() {
        emailService.sendResetEmail("student@example.com", "reset token");

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertEquals("VulnerableApp password reset", message.getSubject());
        assertEquals(
                "Use the following link to reset your password:"
                        + " http://localhost/reset-password?token=reset+token",
                message.getText());
    }

    @Test
    void shouldSendVerificationEmailWithTokenLink() {
        emailService.sendVerificationEmail("student@example.com", "verify token");

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();
        assertEquals("VulnerableApp email verification", message.getSubject());
        assertEquals(
                "Use the following link to verify your email address:"
                        + " http://localhost/verify-email?token=verify+token",
                message.getText());
    }
}
