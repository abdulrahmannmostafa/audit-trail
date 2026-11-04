package com.qeema.util.notification.utility;

import java.util.List;

import org.springframework.stereotype.Component;

import com.qeema.util.notification.dto.SendEmailRequest;

@Component
public class EmailUtility {

    public List<String> copyRecipients(List<String> recipients) {
        return recipients == null || recipients.isEmpty() ? null : List.copyOf(recipients);
    }

    public void validate(SendEmailRequest request) {
        if (request == null || request.getTo() == null || request.getTo().isEmpty()) {
            throw new IllegalArgumentException("At least one recipient is required");
        }
        validateRecipients(request.getTo(), "TO");
        validateOptionalRecipients(request.getCc(), "CC");
        validateOptionalRecipients(request.getBcc(), "BCC");

        boolean hasTemplate = request.getTemplateName() != null && !request.getTemplateName().isBlank();
        if (!hasTemplate && (request.getSubject() == null || request.getSubject().isBlank()
                || request.getBody() == null || request.getBody().isBlank())) {
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
}
