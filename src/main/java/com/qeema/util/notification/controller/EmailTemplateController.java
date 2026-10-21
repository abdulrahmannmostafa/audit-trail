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

import com.qeema.util.notification.dto.EmailTemplateResponse;
import com.qeema.util.notification.dto.EmailTemplateRequest;
import com.qeema.util.notification.dto.PreviewEmailRequest;
import com.qeema.util.notification.dto.PreviewEmailResponse;
import com.qeema.util.notification.service.EmailTemplateService;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/admin/email-templates")
public class EmailTemplateController {

    private final EmailTemplateService templateService;

    public EmailTemplateController(EmailTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public List<EmailTemplateResponse> getTemplates() {
        return templateService.getTemplates();
    }

    @PostMapping("/preview")
    public PreviewEmailResponse preview(
            @Valid @RequestBody PreviewEmailRequest request) {
        return templateService.preview(
                request.templateName(),
                request.variables());
    }

    @PostMapping
    public EmailTemplateResponse createTemplate(
            @Valid @RequestBody EmailTemplateRequest request) {
        return templateService.create(request);
    }

    @GetMapping("/{name}")
    public EmailTemplateResponse getTemplate(@PathVariable String name) {
        return templateService.getTemplate(name);
    }

    @PutMapping("/{name}")
    public EmailTemplateResponse updateTemplate(
            @PathVariable String name,
            @Valid @RequestBody EmailTemplateRequest request) {
        return templateService.update(name, request);
    }

    @DeleteMapping("/{name}")
    public void deleteTemplate(@PathVariable String name) {
        templateService.delete(name);
    }
}
