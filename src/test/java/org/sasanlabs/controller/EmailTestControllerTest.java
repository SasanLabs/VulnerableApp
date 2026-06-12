package org.sasanlabs.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sasanlabs.service.email.EmailService;
import org.springframework.mail.MailSendException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class EmailTestControllerTest {

    @Mock private EmailService emailService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new EmailTestController(emailService)).build();
    }

    @Test
    void shouldSendPlainTextTestEmail() throws Exception {
        mockMvc.perform(
                        post("/email/test")
                                .param("to", "student@example.com")
                                .param("subject", "Subject")
                                .param("body", "Body"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("sent"))
                .andExpect(jsonPath("$.to").value("student@example.com"));

        verify(emailService).sendEmail("student@example.com", "Subject", "Body");
    }

    @Test
    void shouldSendHtmlTestEmail() throws Exception {
        mockMvc.perform(
                        post("/email/test")
                                .param("to", "student@example.com")
                                .param("subject", "Subject")
                                .param("body", "<b>Body</b>")
                                .param("html", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("sent"))
                .andExpect(jsonPath("$.to").value("student@example.com"));

        verify(emailService).sendHtmlEmail("student@example.com", "Subject", "<b>Body</b>");
    }

    @Test
    void shouldReturnBadRequestWhenEmailServiceRejectsInput() throws Exception {
        doThrow(new IllegalArgumentException("Invalid email address: invalid-address"))
                .when(emailService)
                .sendEmail("invalid-address", "Subject", "Body");

        mockMvc.perform(
                        post("/email/test")
                                .param("to", "invalid-address")
                                .param("subject", "Subject")
                                .param("body", "Body"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("Invalid email address: invalid-address"));
    }

    @Test
    void shouldReturnServerErrorWhenEmailSendingFails() throws Exception {
        doThrow(new MailSendException("SMTP unavailable"))
                .when(emailService)
                .sendEmail("student@example.com", "Subject", "Body");

        mockMvc.perform(
                        post("/email/test")
                                .param("to", "student@example.com")
                                .param("subject", "Subject")
                                .param("body", "Body"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.error").value("Unable to send test email"));
    }
}
