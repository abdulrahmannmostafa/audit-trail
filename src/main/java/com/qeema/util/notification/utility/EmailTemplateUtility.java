package com.qeema.util.notification.utility;

import org.springframework.stereotype.Component;

import com.qeema.util.common.exception.ResourceNotFoundException;
import com.qeema.util.notification.dto.EmailTemplateResponse;
import com.qeema.util.notification.entity.EmailTemplate;
import com.qeema.util.notification.mapper.EmailTemplateMapper;
import com.qeema.util.notification.repository.EmailTemplateRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailTemplateUtility {

    private final EmailTemplateRepository repository;
    private final EmailTemplateMapper mapper;

    public EmailTemplate findEnabledTemplate(String name) {
        String normalizedName = normalize(name);
        return repository.findByName(normalizedName)
                .orElseThrow(() -> new ResourceNotFoundException("Email template not found: " + normalizedName));
    }

    public String normalize(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Template name must not be blank");
        }
        return name.trim();
    }

    public EmailTemplateResponse toResponse(EmailTemplate template) {
        return mapper.toResponse(template);
    }
}
