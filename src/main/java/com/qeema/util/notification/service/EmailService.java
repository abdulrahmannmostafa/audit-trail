package com.qeema.util.notification.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
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
import com.qeema.util.notification.utility.EmailDeliveryUtility;
import com.qeema.util.notification.utility.EmailUtility;

import jakarta.mail.MessagingException;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final EmailLogRepository logRepository;
    private final EmailTemplateUseCase templateService;
    private final EmailTemplateRenderer renderer;

    private final AuditService auditService;
    private final EmailUtility emailUtility;
    private final EmailDeliveryUtility deliveryUtility;

    public SendEmailResponse sendEmail(SendEmailRequest request, Authentication authentication) {

        emailUtility.validate(request);

        // Resolve the template if provided
        EmailTemplate template = request.getTemplateName() == null || request.getTemplateName().isBlank()
                ? null
                : templateService.getEnabledTemplate(request.getTemplateName());

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
                emailUtility.copyRecipients(request.getCc()), emailUtility.copyRecipients(request.getBcc()), subject));

        try {
            deliveryUtility.sendEmail(request, subject, body, html);

            log.setStatus(NotificationConstants.SENT);

            log.setSentAt(Instant.now());

            logRepository.save(log);

            auditService.recordAuditEventWithSnapshots(authentication, NotificationConstants.EMAIL_SENT,
                    NotificationConstants.EMAIL,
                    log.getId(), NotificationConstants.SUCCESS,
                    Map.of("status", NotificationConstants.PENDING), Map.of("status", NotificationConstants.SENT),
                    Map.of("recipientCount", request.getTo().size(), "template",
                            template == null ? NotificationConstants.PLAIN_TEXT : template.getName()));

            return new SendEmailResponse(log.getId(), NotificationConstants.SENT, subject);
        } catch (MessagingException | MailException exception) {

            try {
                deliveryUtility.markEmailAsFailed(log, exception);
            } catch (RuntimeException logException) {
                logger.error("Failed to persist email failure log {}", log.getId(), logException);
            }
            try {

                auditService.recordAuditEventWithSnapshots(authentication, NotificationConstants.EMAIL_SENT,
                        NotificationConstants.EMAIL,
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

}
