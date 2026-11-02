package com.qeema.util.notification.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreviewEmailRequest {
    @NotBlank(message = "Template name must not be blank")
    private String templateName;
    private Map<String, Object> variables;
}
