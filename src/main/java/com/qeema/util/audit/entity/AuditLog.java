package com.qeema.util.audit.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.qeema.util.audit.messaging.AuditEvent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "event_id", length = 36, nullable = false, unique = true)
    private String eventId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "actor_id", nullable = false, length = 100)
    private String actorId;

    @Column(name = "actor_username", length = 255)
    private String actorUsername;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(nullable = false, length = 20)
    private String outcome;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_values", columnDefinition = "json")
    private Map<String, Object> oldValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_values", columnDefinition = "json")
    private Map<String, Object> newValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> metadata;

    @Column(name = "consumed_at", nullable = false, insertable = false, updatable = false)
    private Instant consumedAt;

    @PrePersist
    void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }

    @Builder
    private AuditLog(AuditEvent event) {
        eventId = event.getEventId().toString();
        occurredAt = event.getOccurredAt();
        actorId = event.getActorId();
        actorUsername = event.getActorUsername();
        action = event.getAction();
        entityType = event.getEntityType();
        entityId = event.getEntityId();
        outcome = event.getOutcome();
        oldValues = event.getOldValues();
        newValues = event.getNewValues();
        metadata = event.getMetadata();
    }

    public static AuditLog from(AuditEvent event) {
        return new AuditLog(event);
    }

}
