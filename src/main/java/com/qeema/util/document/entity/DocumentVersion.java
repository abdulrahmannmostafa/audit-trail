package com.qeema.util.document.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "document_versions", uniqueConstraints = @UniqueConstraint(name = "uq_document_version", columnNames = {
        "document_id", "version_number" }))
public class DocumentVersion {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "document_id", nullable = false, length = 36)
    private String documentId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "storage_key", nullable = false, length = 512, unique = true)
    private String storageKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 127)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Column(name = "uploaded_by", nullable = false, length = 36)
    private String uploadedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }

    protected DocumentVersion() {
    }

    public DocumentVersion(
            String documentId,
            int versionNumber,
            String storageKey,
            String originalFilename,
            String contentType,
            long sizeBytes,
            String checksumSha256,
            String uploadedBy) {
        this.documentId = documentId;
        this.versionNumber = versionNumber;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.checksumSha256 = checksumSha256;
        this.uploadedBy = uploadedBy;
    }

    public String getId() {
        return id;
    }

    public String getDocumentId() {
        return documentId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
