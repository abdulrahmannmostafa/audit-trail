package com.qeema.util.audit.messaging;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEvent(
        UUID eventId,
        Instant occurredAt,
        String actorId,
        String actorUsername,
        String action,
        String entityType,
        String entityId,
        String outcome,
        Map<String, Object> oldValues,
        Map<String, Object> newValues,
        Map<String, Object> metadata) {

    public AuditEvent {
        if (eventId == null || occurredAt == null
                || actorId == null || actorId.isBlank()
                || action == null || action.isBlank()
                || entityType == null || entityType.isBlank()
                || outcome == null || outcome.isBlank()) {
            throw new IllegalArgumentException("Audit event contains invalid required fields");
        }
        oldValues = oldValues == null ? Map.of() : Map.copyOf(oldValues);
        newValues = newValues == null ? Map.of() : Map.copyOf(newValues);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
