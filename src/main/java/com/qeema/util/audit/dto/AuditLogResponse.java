package com.qeema.util.audit.dto;

import java.time.Instant;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private String eventId;
    private Instant occurredAt;
    private String actorId;
    private String actorUsername;
    private String action;
    private String entityType;
    private String entityId;
    private String outcome;
    private Map<String, Object> oldValues;
    private Map<String, Object> newValues;
    private Map<String, Object> metadata;
    private Instant consumedAt;
}
