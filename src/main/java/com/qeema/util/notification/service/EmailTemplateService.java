package com.qeema.util.notification.service;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import com.qeema.util.audit.service.AuditService;
import com.qeema.util.common.exception.ResourceNotFoundException;
import com.qeema.util.notification.dto.EmailTemplateRequest;
import com.qeema.util.notification.dto.EmailTemplateResponse;
import com.qeema.util.notification.dto.PreviewEmailResponse;
import com.qeema.util.notification.entity.EmailTemplate;
import com.qeema.util.notification.repository.EmailTemplateRepository;
import com.qeema.util.notification.constants.NotificationConstants;
import com.qeema.util.notification.mapper.EmailTemplateMapper;

@Service
@RequiredArgsConstructor
public class EmailTemplateService implements EmailTemplateUseCase {

    private final EmailTemplateRepository templateRepository;
    private final EmailTemplateRenderer renderer;
    private final AuditService auditService;
    private final EmailTemplateMapper templateMapper;

    @Transactional(readOnly = true)
    public List<EmailTemplateResponse> getTemplates(Authentication authentication) {
        List<EmailTemplateResponse> templates = templateRepository.findAll().stream().map(this::toResponse).toList();

        auditService.record(authentication, NotificationConstants.TEMPLATES_LISTED,
                NotificationConstants.EMAIL_TEMPLATE,
                NotificationConstants.COLLECTION, NotificationConstants.SUCCESS, Map.of("count", templates.size()));

        return templates;
    }

    @Transactional(readOnly = true)
    public PreviewEmailResponse preview(String name, Map<String, Object> variables, Authentication authentication) {
        EmailTemplate template = getEnabledTemplate(name);

        auditService.record(authentication, NotificationConstants.TEMPLATE_PREVIEWED,
                NotificationConstants.EMAIL_TEMPLATE,
                NotificationConstants.PREVIEW, NotificationConstants.SUCCESS, Map.of("name", template.getName()));

        return new PreviewEmailResponse(template.getName(),
                renderer.renderSubject(template.getSubjectTemplate(), variables),
                renderer.renderHtml(template.getHtmlContent(), variables));
    }

    @Transactional(readOnly = true)
    public EmailTemplateResponse getTemplate(String name, Authentication authentication) {
        EmailTemplateResponse response = toResponse(templateRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Email template not found: " + name)));

        auditService.record(authentication, NotificationConstants.TEMPLATE_VIEWED, NotificationConstants.EMAIL_TEMPLATE,
                NotificationConstants.SINGLE, NotificationConstants.SUCCESS, Map.of("name", response.getName()));

        return response;
    }

    @Transactional
    public EmailTemplateResponse create(EmailTemplateRequest request, Authentication authentication) {
        String name = normalizeName(request.getName());

        if (templateRepository.existsByName(name)) {
            throw new IllegalArgumentException("Email template already exists: " + name);
        }

        EmailTemplate template = new EmailTemplate(name, request.getSubjectTemplate().trim(), request.getHtmlContent(),
                request.isEnabled());

        EmailTemplate saved = templateRepository.save(template);

        auditService.record(authentication, NotificationConstants.TEMPLATE_CREATED,
                NotificationConstants.EMAIL_TEMPLATE,
                NotificationConstants.SINGLE, NotificationConstants.SUCCESS, Map.of(),
                Map.of("name", saved.getName(), "enabled", saved.isEnabled()), Map.of("name", saved.getName()));

        return toResponse(saved);
    }

    @Transactional
    public EmailTemplateResponse update(String name, EmailTemplateRequest request, Authentication authentication) {
        EmailTemplate template = findTemplate(name);

        String updatedName = normalizeName(request.getName());

        if (templateRepository.existsByNameAndIdNot(updatedName, template.getId())) {
            throw new IllegalArgumentException("Email template already exists: " + updatedName);
        }

        template.setName(updatedName);
        template.setSubjectTemplate(request.getSubjectTemplate().trim());
        template.setHtmlContent(request.getHtmlContent());
        template.setEnabled(request.isEnabled());

        EmailTemplate saved = templateRepository.save(template);

        auditService.record(authentication, NotificationConstants.TEMPLATE_UPDATED,
                NotificationConstants.EMAIL_TEMPLATE,
                NotificationConstants.SINGLE, NotificationConstants.SUCCESS,
                Map.of("name", template.getName(), "enabled", template.isEnabled()),
                Map.of("name", saved.getName(), "enabled", saved.isEnabled()), Map.of("name", saved.getName()));

        return toResponse(saved);
    }

    @Transactional
    public void delete(String name, Authentication authentication) {
        EmailTemplate template = findTemplate(name);

        templateRepository.delete(template);

        auditService.record(authentication, NotificationConstants.TEMPLATE_DELETED,
                NotificationConstants.EMAIL_TEMPLATE,
                NotificationConstants.SINGLE, NotificationConstants.SUCCESS,
                Map.of("name", template.getName(), "enabled", template.isEnabled()), Map.of(),
                Map.of("name", template.getName()));
    }

    @Transactional(readOnly = true)
    public EmailTemplate getEnabledTemplate(String name) {
        EmailTemplate template = findTemplate(name);

        if (!template.isEnabled()) {
            throw new IllegalArgumentException("Email template is disabled: " + name);
        }

        return template;
    }

    private EmailTemplate findTemplate(String name) {
        String normalizedName = normalizeName(name);
        return templateRepository.findByName(normalizedName)
                .orElseThrow(() -> new ResourceNotFoundException("Email template not found: " + normalizedName));
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Template name must not be blank");
        }
        return name.trim();
    }

    private EmailTemplateResponse toResponse(EmailTemplate template) {
        return templateMapper.toResponse(template);
    }
}
