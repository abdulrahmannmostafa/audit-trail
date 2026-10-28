package com.qeema.util.document.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qeema.util.document.entity.DocumentPermission;

public interface DocumentPermissionRepository
        extends JpaRepository<DocumentPermission, String> {

    boolean existsByDocumentIdAndSubjectTypeAndSubjectIdAndPermission(
            String documentId,
            String subjectType,
            String subjectId,
            String permission);
}
