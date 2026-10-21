package com.qeema.util.notification.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qeema.util.common.exception.ResourceNotFoundException;
import com.qeema.util.notification.dto.EmailTemplateResponse;
import com.qeema.util.notification.dto.EmailTemplateRequest;
import com.qeema.util.notification.dto.PreviewEmailResponse;
import com.qeema.util.notification.entity.EmailTemplate;
import com.qeema.util.notification.repository.EmailTemplateRepository;

@Service
public class EmailTemplateService {

    private final EmailTemplateRepository templateRepository;
    private final EmailTemplateRenderer renderer;

    public EmailTemplateService(
            EmailTemplateRepository templateRepository,
            EmailTemplateRenderer renderer) {
        this.templateRepository = templateRepository;
        this.renderer = renderer;
    }

    @Transactional(readOnly = true)
    public List<EmailTemplateResponse> getTemplates() {
        return templateRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PreviewEmailResponse preview(
            String name,
            Map<String, Object> variables) {
        EmailTemplate template = getEnabledTemplate(name);
        return new PreviewEmailResponse(
                template.getName(),
                renderer.renderSubject(
                        template.getSubjectTemplate(), variables),
                renderer.renderHtml(template.getHtmlContent(), variables));
    }

    @Transactional(readOnly = true)
    public EmailTemplateResponse getTemplate(String name) {
        return toResponse(templateRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Email template not found: " + name)));
    }

    @Transactional
    public EmailTemplateResponse create(EmailTemplateRequest request) {
        String name = normalizeName(request.name());
        if (templateRepository.existsByName(name)) {
            throw new IllegalArgumentException(
                    "Email template already exists: " + name);
        }

        EmailTemplate template = new EmailTemplate(
                name,
                request.subjectTemplate().trim(),
                request.htmlContent(),
                request.enabled());
        return toResponse(templateRepository.save(template));
    }

    @Transactional
    public EmailTemplateResponse update(
            String name,
            EmailTemplateRequest request) {
        EmailTemplate template = findTemplate(name);
        String updatedName = normalizeName(request.name());

        if (templateRepository.existsByNameAndIdNot(
                updatedName, template.getId())) {
            throw new IllegalArgumentException(
                    "Email template already exists: " + updatedName);
        }

        template.setName(updatedName);
        template.setSubjectTemplate(request.subjectTemplate().trim());
        template.setHtmlContent(request.htmlContent());
        template.setEnabled(request.enabled());
        return toResponse(templateRepository.save(template));
    }

    @Transactional
    public void delete(String name) {
        templateRepository.delete(findTemplate(name));
    }

    @Transactional(readOnly = true)
    public EmailTemplate getEnabledTemplate(String name) {
        EmailTemplate template = findTemplate(name);

        if (!template.isEnabled()) {
            throw new IllegalArgumentException(
                    "Email template is disabled: " + name);
        }
        return template;
    }

    private EmailTemplate findTemplate(String name) {
        String normalizedName = normalizeName(name);
        return templateRepository.findByName(normalizedName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Email template not found: " + normalizedName));
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Template name must not be blank");
        }
        return name.trim();
    }

    private EmailTemplateResponse toResponse(EmailTemplate template) {
        return new EmailTemplateResponse(
                template.getId(),
                template.getName(),
                template.getSubjectTemplate(),
                template.isEnabled());
    }
}
