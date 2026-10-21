package com.qeema.util.notification.dto;

import java.util.List;

public record EmailRequest(
        List<String> to,
        List<String> cc,
        List<String> bcc,
        String subject,
        String body) {
}