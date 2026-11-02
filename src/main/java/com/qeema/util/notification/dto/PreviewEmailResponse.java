package com.qeema.util.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreviewEmailResponse {
    private String templateName;
    private String subject;
    private String htmlContent;
}
