package com.qeema.util.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplateRequest {
    @NotBlank(message = "Template name must not be blank")
    @Size(max = 100, message = "Template name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Subject template must not be blank")
    @Size(max = 255, message = "Subject template must not exceed 255 characters")
    private String subjectTemplate;

    @NotBlank(message = "HTML content must not be blank")
    private String htmlContent;

    private boolean enabled;
}
