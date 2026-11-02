package com.qeema.util.notification.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailRequest {
    @NotEmpty(message = "At least one recipient is required")
    private List<@NotBlank(message = "Recipient must not be blank") @Email(message = "Recipient must be a valid email address") String> to;
    private List<@NotBlank(message = "CC recipient must not be blank") @Email(message = "CC recipient must be a valid email address") String> cc;
    private List<@NotBlank(message = "BCC recipient must not be blank") @Email(message = "BCC recipient must be a valid email address") String> bcc;
    private String subject;
    private String body;
    private String templateName;
    private Map<String, Object> variables;
}
