package com.qeema.util.notification.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.qeema.util.notification.dto.SendEmailRequest;
import com.qeema.util.notification.dto.SendEmailResponse;
import com.qeema.util.notification.service.EmailService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/emails")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SendEmailResponse sendEmail(
            @Valid @RequestBody SendEmailRequest request) {
        return emailService.sendEmail(request);
    }
}
