package com.qeema.util.notification.service;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;

import com.qeema.util.notification.dto.EmailTemplateRequest;
import com.qeema.util.notification.dto.EmailTemplateResponse;
import com.qeema.util.notification.dto.PreviewEmailResponse;
import com.qeema.util.notification.entity.EmailTemplate;

public interface EmailTemplateUseCase {
    List<EmailTemplateResponse> getTemplates(Authentication authentication);

    PreviewEmailResponse preview(String name, Map<String, Object> variables, Authentication authentication);

    EmailTemplateResponse getTemplate(String name, Authentication authentication);

    EmailTemplate getEnabledTemplate(String name);

    EmailTemplateResponse create(EmailTemplateRequest request, Authentication authentication);

    EmailTemplateResponse update(String name, EmailTemplateRequest request, Authentication authentication);

    void delete(String name, Authentication authentication);
}
