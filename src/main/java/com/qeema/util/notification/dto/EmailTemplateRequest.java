package com.qeema.util.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailTemplateRequest(
        @NotBlank(message = "Template name must not be blank")
        @Size(max = 100, message = "Template name must not exceed 100 characters")
        String name,
        @NotBlank(message = "Subject template must not be blank")
        @Size(max = 255, message = "Subject template must not exceed 255 characters")
        String subjectTemplate,
        @NotBlank(message = "HTML content must not be blank")
        String htmlContent,
        boolean enabled) {
}
