package com.qeema.util.notification.dto;

public record PreviewEmailResponse(
        String templateName,
        String subject,
        String htmlContent) {
}
