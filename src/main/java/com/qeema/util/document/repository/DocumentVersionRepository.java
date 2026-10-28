package com.qeema.util.document.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qeema.util.document.entity.DocumentVersion;

public interface DocumentVersionRepository
                extends JpaRepository<DocumentVersion, String> {

        List<DocumentVersion> findByDocumentIdOrderByVersionNumberDesc(
                        String documentId);

        Optional<DocumentVersion> findByDocumentIdAndId(
                        String documentId,
                        String id);

        Optional<DocumentVersion> findTopByDocumentIdOrderByVersionNumberDesc(
                        String documentId);
}
