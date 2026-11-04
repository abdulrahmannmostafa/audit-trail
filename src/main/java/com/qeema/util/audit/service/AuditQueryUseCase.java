package com.qeema.util.audit.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;

import com.qeema.util.audit.entity.AuditLog;

public interface AuditQueryUseCase {
        Page<AuditLog> searchAuditLogs(String actorId, String action, String entityType,
                        String entityId, Instant from, Instant to, int page, int size);

        Page<AuditLog> searchForAuditLogs(Authentication authentication, String actorId,
                        String action, String entityType, String entityId, Instant from,
                        Instant to, int page, int size);

        AuditLog get(String eventId);
}
