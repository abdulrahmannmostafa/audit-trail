package com.qeema.util.audit.dto;

import java.time.Instant;
import java.util.Map;

public record AuditLogResponse(
        String eventId,
        Instant occurredAt,
        String actorId,
        String actorUsername,
        String action,
        String entityType,
        String entityId,
        String outcome,
        Map<String, Object> oldValues,
        Map<String, Object> newValues,
        Map<String, Object> metadata,
        Instant consumedAt) {
}
