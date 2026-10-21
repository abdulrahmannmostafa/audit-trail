package com.qeema.util.notification.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.qeema.util.common.exception.EmailSendingException;
import com.qeema.util.notification.dto.SendEmailRequest;
import com.qeema.util.notification.dto.SendEmailResponse;
import com.qeema.util.notification.entity.EmailLog;
import com.qeema.util.notification.entity.EmailTemplate;
import com.qeema.util.notification.repository.EmailLogRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final String SENT_STATUS = "SENT";
    private final String FAILED_STATUS = "FAILED";

    private final JavaMailSender mailSender;
    private final EmailLogRepository logRepository;
    private final EmailTemplateService templateService;
    private final EmailTemplateRenderer renderer;
    private final String fromAddress;

    public EmailService(
            JavaMailSender mailSender,
            EmailLogRepository logRepository,
            EmailTemplateService templateService,
            EmailTemplateRenderer renderer,
            @Value("${app.mail.from:no-reply@example.com}") String fromAddress) {
        this.mailSender = mailSender;
        this.logRepository = logRepository;
        this.templateService = templateService;
        this.renderer = renderer;
        this.fromAddress = fromAddress;
    }

    public SendEmailResponse sendEmail(SendEmailRequest request) {
        validate(request);

        // Resolve the template if provided
        EmailTemplate template = resolveTemplate(request.templateName());

        // Prepare the variables for rendering
        Map<String, Object> variables = request.variables() == null
                ? Map.of()
                : request.variables();

        // Render subject and body based on the template or use provided values
        String subject = template == null
                ? request.subject()
                : renderer.renderSubject(
                        template.getSubjectTemplate(), variables);

        // Render the body based on the template or use provided value
        String body = template == null
                ? request.body()
                : renderer.renderHtml(template.getHtmlContent(), variables);

        // Mask the body if the template is used and the body is empty
        boolean html = template != null;

        EmailLog log = logRepository.save(new EmailLog(
                template,
                List.copyOf(request.to()),
                copyRecipients(request.cc()),
                copyRecipients(request.bcc()),
                subject));

        try {
            sendMessage(request, subject, body, html);
            log.setStatus(SENT_STATUS);
            log.setSentAt(Instant.now());
            logRepository.save(log);

            return new SendEmailResponse(log.getId(), SENT_STATUS, subject);
        } catch (MessagingException | MailException exception) {
            markFailed(log, exception);
            throw new EmailSendingException("Failed to send email", exception);
        }
    }

    private void sendMessage(
            SendEmailRequest request,
            String subject,
            String body,
            boolean html)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message,
                false,
                "UTF-8");

        helper.setFrom(fromAddress);
        helper.setTo(request.to().toArray(String[]::new));

        if (request.cc() != null && !request.cc().isEmpty()) {
            helper.setCc(request.cc().toArray(String[]::new));
        }
        if (request.bcc() != null && !request.bcc().isEmpty()) {
            helper.setBcc(request.bcc().toArray(String[]::new));
        }

        helper.setSubject(subject);
        helper.setText(body, html);
        mailSender.send(message);
    }

    private EmailTemplate resolveTemplate(String templateName) {
        if (templateName == null || templateName.isBlank()) {
            return null;
        }
        return templateService.getEnabledTemplate(templateName);
    }

    private void validate(SendEmailRequest request) {
        if (request == null || request.to() == null || request.to().isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one recipient is required");
        }

        validateRecipients(request.to(), "TO");
        validateOptionalRecipients(request.cc(), "CC");
        validateOptionalRecipients(request.bcc(), "BCC");

        boolean hasTemplate = request.templateName() != null
                && !request.templateName().isBlank();
        if (!hasTemplate
                && (request.subject() == null || request.subject().isBlank()
                        || request.body() == null
                        || request.body().isBlank())) {
            throw new IllegalArgumentException(
                    "Subject and body are required when no template is used");
        }
    }

    private void validateOptionalRecipients(
            List<String> recipients,
            String field) {
        if (recipients != null && !recipients.isEmpty()) {
            validateRecipients(recipients, field);
        }
    }

    private void validateRecipients(
            List<String> recipients,
            String field) {
        if (recipients.stream().anyMatch(
                email -> email == null || email.isBlank())) {
            throw new IllegalArgumentException(
                    field + " addresses must not be blank");
        }
    }

    private List<String> copyRecipients(List<String> recipients) {
        return recipients == null || recipients.isEmpty()
                ? null
                : List.copyOf(recipients);
    }

    private void markFailed(EmailLog log, Exception exception) {
        log.setStatus(FAILED_STATUS);
        log.setErrorMessage(exception.getMessage());
        try {
            logRepository.save(log);
        } catch (DataAccessException logException) {
            logger.error(
                    "Failed to persist email failure log {}",
                    log.getId(),
                    logException);
        }
        logger.error("Failed to send email log {}", log.getId(), exception);
    }
}
