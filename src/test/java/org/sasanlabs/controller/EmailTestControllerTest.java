package org.sasanlabs.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sasanlabs.service.email.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;

@ExtendWith(MockitoExtension.class)
class EmailTestControllerTest {

    @Mock private EmailService emailService;

    @Test
    void shouldSendPlainTextTestEmail() {
        EmailTestController controller = new EmailTestController(emailService);

        ResponseEntity<Map<String, String>> response =
                controller.sendTestEmail("student@example.com", "Subject", "Body", false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("sent", response.getBody().get("status"));
        assertEquals("student@example.com", response.getBody().get("to"));

        verify(emailService).sendEmail("student@example.com", "Subject", "Body");
    }

    @Test
    void shouldSendHtmlTestEmail() {
        EmailTestController controller = new EmailTestController(emailService);

        ResponseEntity<Map<String, String>> response =
                controller.sendTestEmail("student@example.com", "Subject", "<b>Body</b>", true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("sent", response.getBody().get("status"));
        assertEquals("student@example.com", response.getBody().get("to"));

        verify(emailService).sendHtmlEmail("student@example.com", "Subject", "<b>Body</b>");
    }

    @Test
    void shouldReturnBadRequestWhenEmailServiceRejectsInput() {
        doThrow(new IllegalArgumentException("Invalid email address: invalid-address"))
                .when(emailService)
                .sendEmail("invalid-address", "Subject", "Body");

        EmailTestController controller = new EmailTestController(emailService);
        ResponseEntity<Map<String, String>> response =
                controller.sendTestEmail("invalid-address", "Subject", "Body", false);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("failed", response.getBody().get("status"));
        assertEquals("Invalid email address: invalid-address", response.getBody().get("error"));
    }

    @Test
    void shouldReturnServerErrorWhenEmailSendingFails() {
        doThrow(new MailSendException("SMTP unavailable"))
                .when(emailService)
                .sendEmail("student@example.com", "Subject", "Body");

        EmailTestController controller = new EmailTestController(emailService);
        ResponseEntity<Map<String, String>> response =
                controller.sendTestEmail("student@example.com", "Subject", "Body", false);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("failed", response.getBody().get("status"));
        assertEquals("Unable to send test email", response.getBody().get("error"));
    }

        @Test
        void shouldSendResetEmail() {
                EmailTestController controller = new EmailTestController(emailService);

                ResponseEntity<Map<String, String>> response =
                                controller.sendResetEmail("student@example.com", "reset token");

                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals("sent", response.getBody().get("status"));
                assertEquals("student@example.com", response.getBody().get("to"));

                verify(emailService).sendResetEmail("student@example.com", "reset token");
        }

        @Test
        void shouldSendVerificationEmail() {
                EmailTestController controller = new EmailTestController(emailService);

                ResponseEntity<Map<String, String>> response =
                                controller.sendVerificationEmail("student@example.com", "verify token");

                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals("sent", response.getBody().get("status"));
                assertEquals("student@example.com", response.getBody().get("to"));

                verify(emailService).sendVerificationEmail("student@example.com", "verify token");
        }

        @Test
        void shouldReturnBadRequestWhenResetEmailInputIsRejected() {
                doThrow(new IllegalArgumentException("token must not be blank"))
                                .when(emailService)
                                .sendResetEmail("student@example.com", " ");

                EmailTestController controller = new EmailTestController(emailService);
                ResponseEntity<Map<String, String>> response =
                                controller.sendResetEmail("student@example.com", " ");

                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                assertEquals("failed", response.getBody().get("status"));
                assertEquals("token must not be blank", response.getBody().get("error"));
        }
}
