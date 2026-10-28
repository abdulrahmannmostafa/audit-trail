CREATE TABLE documents (
    id CHAR(36) NOT NULL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    owner_id VARCHAR(255) NOT NULL,
    current_version_id CHAR(36) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,

    INDEX idx_documents_owner (owner_id),
    INDEX idx_documents_status (status),
    INDEX idx_documents_updated (updated_at)
) ENGINE=InnoDB;

CREATE TABLE document_versions (
    id CHAR(36) NOT NULL PRIMARY KEY,
    document_id CHAR(36) NOT NULL,
    version_number INT UNSIGNED NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(127) NOT NULL,
    size_bytes BIGINT UNSIGNED NOT NULL,
    checksum_sha256 CHAR(64) NOT NULL,
    uploaded_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_document_versions_document
        FOREIGN KEY (document_id) REFERENCES documents(id),
    CONSTRAINT uq_document_version
        UNIQUE (document_id, version_number),
    CONSTRAINT uq_document_storage_key UNIQUE (storage_key),
    INDEX idx_document_versions_created (document_id, created_at)
) ENGINE=InnoDB;

ALTER TABLE documents
    ADD CONSTRAINT fk_documents_current_version
    FOREIGN KEY (current_version_id)
    REFERENCES document_versions(id)
    ON DELETE SET NULL;

CREATE TABLE document_permissions (
    id CHAR(36) NOT NULL PRIMARY KEY,
    document_id CHAR(36) NOT NULL,
    subject_type VARCHAR(10) NOT NULL,
    subject_id VARCHAR(255) NOT NULL,
    permission VARCHAR(20) NOT NULL,
    granted_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_document_permissions_document
        FOREIGN KEY (document_id) REFERENCES documents(id),
    CONSTRAINT uq_document_permission
        UNIQUE (document_id, subject_type, subject_id, permission),
    INDEX idx_document_permissions_subject (subject_type, subject_id)
) ENGINE=InnoDB;

CREATE TABLE document_audit_logs (
    id CHAR(36) NOT NULL PRIMARY KEY,
    document_id CHAR(36) NOT NULL,
    version_id CHAR(36) NULL,
    actor_id VARCHAR(255) NOT NULL,
    action VARCHAR(40) NOT NULL,
    details JSON NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_document_audit_document
        FOREIGN KEY (document_id) REFERENCES documents(id),
    CONSTRAINT fk_document_audit_version
        FOREIGN KEY (version_id) REFERENCES document_versions(id)
        ON DELETE SET NULL,
    INDEX idx_document_audit_history (document_id, created_at),
    INDEX idx_document_audit_actor (actor_id, created_at)
) ENGINE=InnoDB;
