package com.qeema.util.notification.controller;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import com.qeema.util.notification.dto.EmailTemplateResponse;
import com.qeema.util.notification.dto.EmailTemplateRequest;
import com.qeema.util.notification.dto.PreviewEmailRequest;
import com.qeema.util.notification.dto.PreviewEmailResponse;
import com.qeema.util.notification.service.EmailTemplateUseCase;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Validated
@Tag(name = "Email templates", description = "Manage and preview email templates")
@RequestMapping("/api/admin/email-templates")
public class EmailTemplateController {

    private final EmailTemplateUseCase templateService;

    public EmailTemplateController(EmailTemplateUseCase templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    @Operation(summary = "List email templates")
    public List<EmailTemplateResponse> getTemplates(Authentication authentication) {
        return templateService.getTemplates(authentication);
    }

    @PostMapping("/preview")
    @Operation(summary = "Preview an email template")
    public PreviewEmailResponse preview(@Valid @RequestBody PreviewEmailRequest request, Authentication authentication) {
        return templateService.preview(request.getTemplateName(), request.getVariables(), authentication);
    }

    @PostMapping
    @Operation(summary = "Create an email template")
    public EmailTemplateResponse createTemplate(@Valid @RequestBody EmailTemplateRequest request, Authentication authentication) {
        return templateService.create(request, authentication);
    }

    @GetMapping("/{name}")
    @Operation(summary = "Get an email template")
    public EmailTemplateResponse getTemplate(@PathVariable String name, Authentication authentication) {
        return templateService.getTemplate(name, authentication);
    }

    @PutMapping("/{name}")
    @Operation(summary = "Update an email template")
    public EmailTemplateResponse updateTemplate(@PathVariable String name, @Valid @RequestBody EmailTemplateRequest request, Authentication authentication) {
        return templateService.update(name, request, authentication);
    }

    @DeleteMapping("/{name}")
    @Operation(summary = "Delete an email template")
    public void deleteTemplate(@PathVariable String name, Authentication authentication) {
        templateService.delete(name, authentication);
    }
}
