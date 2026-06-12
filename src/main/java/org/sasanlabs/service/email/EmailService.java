package org.sasanlabs.service.email;

public interface EmailService {

    void sendEmail(String to, String subject, String body);

    void sendHtmlEmail(String to, String subject, String htmlBody);

    void sendResetEmail(String to, String token);

    void sendVerificationEmail(String to, String token);
}
