package com.qeema.util.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplateResponse {
    private String id;
    private String name;
    private String subjectTemplate;
    private String htmlContent;
    private boolean enabled;
}
