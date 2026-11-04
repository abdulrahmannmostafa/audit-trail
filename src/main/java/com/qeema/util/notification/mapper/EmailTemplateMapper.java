package com.qeema.util.notification.mapper;

import org.mapstruct.Mapper;

import com.qeema.util.notification.dto.EmailTemplateResponse;
import com.qeema.util.notification.entity.EmailTemplate;

@Mapper(componentModel = "spring")
public interface EmailTemplateMapper {
    EmailTemplateResponse toResponse(EmailTemplate template);
}
