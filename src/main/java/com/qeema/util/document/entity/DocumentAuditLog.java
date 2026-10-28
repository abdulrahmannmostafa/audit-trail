package com.qeema.util.document.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_audit_logs")
public class DocumentAuditLog {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "document_id", nullable = false, length = 36)
    private String documentId;

    @Column(name = "version_id", length = 36)
    private String versionId;

    @Column(name = "actor_id", nullable = false, length = 36)
    private String actorId;

    @Column(nullable = false, length = 40)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> details;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void assignId() {
        if (id == null)
            id = UUID.randomUUID().toString();
    }

    protected DocumentAuditLog() {
    }

    public DocumentAuditLog(
            String documentId,
            String versionId,
            String actorId,
            String action,
            Map<String, Object> details) {
        this.documentId = documentId;
        this.versionId = versionId;
        this.actorId = actorId;
        this.action = action;
        this.details = details;
    }
}
