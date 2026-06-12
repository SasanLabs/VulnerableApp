package org.sasanlabs.service.email;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final String RESET_PASSWORD_PATH = "/reset-password?token=";
    private static final String VERIFY_EMAIL_PATH = "/verify-email?token=";

    private final JavaMailSender javaMailSender;
    private final EmailConfiguration emailConfiguration;

    public EmailServiceImpl(JavaMailSender javaMailSender, EmailConfiguration emailConfiguration) {
        this.javaMailSender = javaMailSender;
        this.emailConfiguration = emailConfiguration;
        this.emailConfiguration.validate();
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        validateEmailInputs(to, subject, body, "body");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailConfiguration.getFrom());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        javaMailSender.send(message);
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        validateEmailInputs(to, subject, htmlBody, "htmlBody");
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(emailConfiguration.getFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
        } catch (MessagingException ex) {
            throw new MailPreparationException("Failed to prepare HTML email", ex);
        }
        javaMailSender.send(message);
    }

    @Override
    public void sendResetEmail(String to, String token) {
        requireText(token, "token");
        String resetUrl = buildUrl(RESET_PASSWORD_PATH, token);
        sendEmail(
                to,
                "VulnerableApp password reset",
                "Use the following link to reset your password: " + resetUrl);
    }

    @Override
    public void sendVerificationEmail(String to, String token) {
        requireText(token, "token");
        String verificationUrl = buildUrl(VERIFY_EMAIL_PATH, token);
        sendEmail(
                to,
                "VulnerableApp email verification",
                "Use the following link to verify your email address: " + verificationUrl);
    }

    private String buildUrl(String path, String token) {
        String baseUrl = emailConfiguration.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + path + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private void validateEmailInputs(
            String to, String subject, String content, String contentName) {
        requireEmailAddress(to);
        requireText(subject, "subject");
        requireNonNull(content, contentName);
        requireEmailAddress(emailConfiguration.getFrom());
    }

    private void requireEmailAddress(String emailAddress) {
        requireText(emailAddress, "email address");
        try {
            InternetAddress internetAddress = new InternetAddress(emailAddress, true);
            internetAddress.validate();
        } catch (AddressException ex) {
            throw new IllegalArgumentException("Invalid email address: " + emailAddress, ex);
        }
    }

    private void requireText(String value, String fieldName) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private void requireNonNull(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }
}
