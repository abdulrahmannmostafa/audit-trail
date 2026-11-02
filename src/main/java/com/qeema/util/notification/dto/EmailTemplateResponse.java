package com.qeema.util.notification.dto;

public record EmailTemplateResponse(
                String id,
                String name,
                String subjectTemplate,
                String htmlContent,
                boolean enabled) {
}
