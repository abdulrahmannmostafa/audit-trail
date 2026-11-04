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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.qeema.util.common.exception.EmailSendingException;
import com.qeema.util.audit.service.AuditService;
import com.qeema.util.notification.dto.SendEmailRequest;
import com.qeema.util.notification.dto.SendEmailResponse;
import com.qeema.util.notification.entity.EmailLog;
import com.qeema.util.notification.entity.EmailTemplate;
import com.qeema.util.notification.repository.EmailLogRepository;
import com.qeema.util.notification.constants.NotificationConstants;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final EmailLogRepository logRepository;
    private final EmailTemplateUseCase templateService;
    private final EmailTemplateRenderer renderer;

    @Value("${app.mail.from:no-reply@example.com}")
    private String fromAddress;

    private final AuditService auditService;

    public SendEmailResponse sendEmail(SendEmailRequest request, Authentication authentication) {

        validate(request);

        // Resolve the template if provided
        EmailTemplate template = resolveTemplate(request.getTemplateName());

        // Prepare the variables for rendering
        Map<String, Object> variables = request.getVariables() == null ? Map.of() : request.getVariables();

        // Render subject and body based on the template or use provided values
        String subject = template == null ? request.getSubject()
                : renderer.renderSubject(template.getSubjectTemplate(), variables);

        // Render the body based on the template or use provided value
        String body = template == null ? request.getBody() : renderer.renderHtml(template.getHtmlContent(), variables);

        // Mask the body if the template is used and the body is empty
        boolean html = template != null;

        EmailLog log = logRepository.save(new EmailLog(template, List.copyOf(request.getTo()),
                copyRecipients(request.getCc()), copyRecipients(request.getBcc()), subject));

        try {
            sendMessage(request, subject, body, html);

            log.setStatus(NotificationConstants.SENT);

            log.setSentAt(Instant.now());

            logRepository.save(log);

            auditService.record(authentication, NotificationConstants.EMAIL_SENT, NotificationConstants.EMAIL,
                    log.getId(), NotificationConstants.SUCCESS,
                    Map.of("status", NotificationConstants.PENDING), Map.of("status", NotificationConstants.SENT),
                    Map.of("recipientCount", request.getTo().size(), "template",
                            template == null ? NotificationConstants.PLAIN_TEXT : template.getName()));

            return new SendEmailResponse(log.getId(), NotificationConstants.SENT, subject);
        } catch (MessagingException | MailException exception) {

            markFailed(log, exception);
            try {

                auditService.record(authentication, NotificationConstants.EMAIL_SENT, NotificationConstants.EMAIL,
                        log.getId(),
                        NotificationConstants.FAILURE, Map.of("status", NotificationConstants.PENDING),
                        Map.of("status", NotificationConstants.FAILED),
                        Map.of("errorType", exception.getClass().getSimpleName()));
            } catch (RuntimeException auditException) {
                logger.warn("Unable to record failed email audit event {}", log.getId(), auditException);
            }

            throw new EmailSendingException("Failed to send email", exception);
        }
    }

    private void sendMessage(SendEmailRequest request, String subject, String body, boolean html)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

        helper.setFrom(fromAddress);

        helper.setTo(request.getTo().toArray(String[]::new));

        if (request.getCc() != null && !request.getCc().isEmpty()) {
            helper.setCc(request.getCc().toArray(String[]::new));
        }

        if (request.getBcc() != null && !request.getBcc().isEmpty()) {
            helper.setBcc(request.getBcc().toArray(String[]::new));
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
        final String TO_FIELD = "TO";
        final String CC_FIELD = "CC";
        final String BCC_FIELD = "BCC";

        if (request == null || request.getTo() == null || request.getTo().isEmpty()) {
            throw new IllegalArgumentException("At least one recipient is required");
        }

        validateRecipients(request.getTo(), TO_FIELD);
        validateOptionalRecipients(request.getCc(), CC_FIELD);
        validateOptionalRecipients(request.getBcc(), BCC_FIELD);

        boolean hasTemplate = request.getTemplateName() != null && !request.getTemplateName().isBlank();
        if (!hasTemplate && (request.getSubject() == null || request.getSubject().isBlank() || request.getBody() == null
                || request.getBody().isBlank())) {
            throw new IllegalArgumentException("Subject and body are required when no template is used");
        }
    }

    private void validateOptionalRecipients(List<String> recipients, String field) {
        if (recipients != null && !recipients.isEmpty()) {
            validateRecipients(recipients, field);
        }
    }

    private void validateRecipients(List<String> recipients, String field) {
        if (recipients.stream().anyMatch(email -> email == null || email.isBlank())) {
            throw new IllegalArgumentException(field + " addresses must not be blank");
        }
    }

    private List<String> copyRecipients(List<String> recipients) {
        return recipients == null || recipients.isEmpty() ? null : List.copyOf(recipients);
    }

    private void markFailed(EmailLog log, Exception exception) {
        log.setStatus(NotificationConstants.FAILED);

        log.setErrorMessage(exception.getMessage());

        try {
            logRepository.save(log);
        } catch (DataAccessException logException) {
            logger.error("Failed to persist email failure log {}", log.getId(), logException);
        }
        logger.error("Failed to send email log {}", log.getId(), exception);
    }
}
