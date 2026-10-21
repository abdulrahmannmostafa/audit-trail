
package com.qeema.util.notification.controller;

import com.qeema.util.notification.dto.EmailRequest;
import com.qeema.util.notification.dto.EmailResponse;
import com.qeema.util.notification.service.EmailService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/emails")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/send")
    public ResponseEntity<EmailResponse> sendEmail(
            @RequestBody EmailRequest request) {

        emailService.sendEmail(request);

        return ResponseEntity.ok(
                new EmailResponse(
                        "Email accepted by the SMTP server",
                        "ACCEPTED"));
    }
}
