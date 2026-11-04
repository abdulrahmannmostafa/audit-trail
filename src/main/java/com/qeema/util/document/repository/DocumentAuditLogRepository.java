package com.qeema.util.document.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qeema.util.document.entity.DocumentAuditLog;

public interface DocumentAuditLogRepository
                extends JpaRepository<DocumentAuditLog, String> {
}
