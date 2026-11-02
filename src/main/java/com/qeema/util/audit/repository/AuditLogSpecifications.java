package com.qeema.util.audit.repository;

import java.time.Instant;

import org.springframework.data.jpa.domain.Specification;

import com.qeema.util.audit.entity.AuditLog;

public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> actorIs(String actorId) {
        return (root, query, builder) -> actorId == null ? null : builder.equal(root.get("actorId"), actorId);
    }

    public static Specification<AuditLog> actionIs(String action) {
        return (root, query, builder) -> action == null ? null : builder.equal(root.get("action"), action);
    }

    public static Specification<AuditLog> entityTypeIs(String entityType) {
        return (root, query, builder) -> entityType == null ? null : builder.equal(root.get("entityType"), entityType);
    }

    public static Specification<AuditLog> entityIdIs(String entityId) {
        return (root, query, builder) -> entityId == null ? null : builder.equal(root.get("entityId"), entityId);
    }

    public static Specification<AuditLog> occurredFrom(Instant from) {
        return (root, query, builder) -> from == null ? null : builder.greaterThanOrEqualTo(root.get("occurredAt"), from);
    }

    public static Specification<AuditLog> occurredBefore(Instant to) {
        return (root, query, builder) -> to == null ? null : builder.lessThan(root.get("occurredAt"), to);
    }
}
