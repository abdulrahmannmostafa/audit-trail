package com.qeema.util.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailResponse {
    private String id;
    private String status;
    private String subject;
}
