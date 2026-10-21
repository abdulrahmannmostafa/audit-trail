package com.qeema.util.notification.service;

import java.util.List;

import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.qeema.util.common.exception.EmailSendingException;
import com.qeema.util.notification.dto.EmailRequest;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(EmailRequest request) {
        validate(request);

        try {
            // Create a MimeMessage for sending the email
            MimeMessage message = mailSender.createMimeMessage();

            // Use MimeMessageHelper to set the email properties
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom("abdelrahmanmostafag@gmail.com");
            helper.setTo(request.to().toArray(String[]::new));

            if (request.cc() != null && !request.cc().isEmpty()) {
                helper.setCc(request.cc().toArray(String[]::new));
            }

            if (request.bcc() != null && !request.bcc().isEmpty()) {
                helper.setBcc(request.bcc().toArray(String[]::new));
            }

            helper.setSubject(request.subject());
            helper.setText(request.body(), false);

            mailSender.send(message);

        } catch (MessagingException | MailException exception) {
            logger.error("Error occurred while sending email: {}", exception.getMessage(), exception);
            throw new EmailSendingException(
                    "Failed to send email");
        }
    }

    private void validate(EmailRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Email request must not be null");
        }

        if (request.to() == null || request.to().isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one recipient is required");
        }

        if (request.to().stream().anyMatch(
                email -> email == null || email.isBlank())) {
            throw new IllegalArgumentException(
                    "Recipient addresses must not be blank");
        }

        if (request.subject() == null || request.subject().isBlank()) {
            throw new IllegalArgumentException(
                    "Email subject must not be blank");
        }

        if (request.body() == null || request.body().isBlank()) {
            throw new IllegalArgumentException(
                    "Email body must not be blank");
        }

        validateOptionalRecipients(request.cc(), "CC");
        validateOptionalRecipients(request.bcc(), "BCC");
    }

    private void validateOptionalRecipients(
            List<String> recipients, String field) {

        if (recipients != null && recipients.stream().anyMatch(
                email -> email == null || email.isBlank())) {
            throw new IllegalArgumentException(
                    field + " addresses must not be blank");
        }
    }
}