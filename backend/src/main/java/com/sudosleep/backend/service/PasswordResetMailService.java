package com.sudosleep.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends password-reset verification emails via SMTP when {@code spring.mail.host} is configured.
 * If SMTP is intentionally left unconfigured in local development, logs a warning without failing.
 */
@Service
public class PasswordResetMailService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String mailHost;
    private final String mailFrom;

    public PasswordResetMailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                                    @Value("${spring.mail.host:}") String mailHost,
                                    @Value("${app.mail.from:no-reply@sudosleep.app}") String mailFrom) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailHost = mailHost;
        this.mailFrom = mailFrom;
    }

    public void sendResetCode(String recipientEmail, String displayName, String code, long expiresInMinutes) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null || mailHost == null || mailHost.isBlank()) {
            log.warn("SMTP is not configured (MAIL_HOST is blank). Skipping password reset email delivery for {}.",
                    recipientEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(recipientEmail);
            message.setSubject("Sudo Sleep — Password Reset Code");
            message.setText("""
                    Hi %s,

                    Your Sudo Sleep password reset code is: %s

                    This code expires in %d minutes. If you did not request a password reset, you can safely ignore this email.
                    """.formatted(displayName, code, expiresInMinutes));
            sender.send(message);
        } catch (MailException ex) {
            log.error("Failed to send password reset email to {}", recipientEmail, ex);
        }
    }
}
