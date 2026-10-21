package com.qeema.util.notification.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;

public record PreviewEmailRequest(
        @NotBlank(message = "Template name must not be blank") String templateName,
        Map<String, Object> variables) {
}
