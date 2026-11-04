package com.qeema.util.audit.mapper;

import org.mapstruct.Mapper;

import com.qeema.util.audit.dto.AuditLogResponse;
import com.qeema.util.audit.entity.AuditLog;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {
    AuditLogResponse toResponse(AuditLog auditLog);
}
