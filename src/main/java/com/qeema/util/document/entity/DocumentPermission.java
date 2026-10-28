package com.qeema.util.document.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_permissions")
public class DocumentPermission {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "document_id", nullable = false, length = 36)
    private String documentId;

    @Column(name = "subject_type", nullable = false, length = 10)
    private String subjectType;

    @Column(name = "subject_id", nullable = false, length = 255)
    private String subjectId;

    @Column(nullable = false, length = 20)
    private String permission;

    @Column(name = "granted_by", nullable = false, length = 36)
    private String grantedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void assignId() {
        if (id == null)
            id = UUID.randomUUID().toString();
    }
}
