package com.qeema.util.notification.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record SendEmailRequest(
                @NotEmpty(message = "At least one recipient is required") List<@NotBlank(message = "Recipient must not be blank") @Email(message = "Recipient must be a valid email address") String> to,
                List<@NotBlank(message = "CC recipient must not be blank") @Email(message = "CC recipient must be a valid email address") String> cc,
                List<@NotBlank(message = "BCC recipient must not be blank") @Email(message = "BCC recipient must be a valid email address") String> bcc,
                String subject,
                String body,
                String templateName,
                Map<String, Object> variables) {
}
