package com.qeema.util.document.utility;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import com.qeema.util.audit.service.AuditService;
import com.qeema.util.common.exception.DocumentStorageException;
import com.qeema.util.common.exception.ResourceNotFoundException;
import com.qeema.util.document.constants.DocumentConstants;
import com.qeema.util.document.dto.DocumentResponse;
import com.qeema.util.document.dto.DocumentVersionResponse;
import com.qeema.util.document.entity.Document;
import com.qeema.util.document.entity.DocumentAuditLog;
import com.qeema.util.document.entity.DocumentVersion;
import com.qeema.util.document.mapper.DocumentMapper;
import com.qeema.util.document.repository.DocumentAuditLogRepository;
import com.qeema.util.document.repository.DocumentPermissionRepository;
import com.qeema.util.document.repository.DocumentRepository;
import com.qeema.util.document.repository.DocumentVersionRepository;
import com.qeema.util.identity.entity.UserIdentity;
import com.qeema.util.identity.repository.UserIdentityRepository;
import com.qeema.util.storage.service.ObjectStorageGateway;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DocumentUtility {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final DocumentAuditLogRepository auditRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final UserIdentityRepository identityRepository;
    private final ObjectStorageGateway storage;
    private final DocumentMapper documentMapper;

    public DocumentVersion addVersion(Document document, MultipartFile file, String actorId,
            List<String> allowedContentTypes) {
        int nextVersion = versionRepository.findTopByDocumentIdOrderByVersionNumberDesc(document.getId())
                .map(version -> version.getVersionNumber() + 1).orElse(1);
        String storageKey = DocumentConstants.DOCUMENT_KEY_PREFIX + document.getId()
                + DocumentConstants.VERSION_KEY_PREFIX + nextVersion + "/" + UUID.randomUUID();

        try (InputStream input = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            storage.upload(storageKey, new DigestInputStream(input, digest), file.getSize(), contentType(file));
            String checksum = HexFormat.of().formatHex(digest.digest());
            DocumentVersion version = new DocumentVersion(document.getId(), nextVersion, storageKey,
                    safeFilename(file), contentType(file), file.getSize(), checksum, actorId);
            DocumentVersion saved = versionRepository.save(version);
            document.setCurrentVersionId(saved.getId());
            documentRepository.save(document);
            return saved;
        } catch (IOException | NoSuchAlgorithmException | RuntimeException exception) {
            throw new DocumentStorageException("Unable to store document content", exception);
        }
    }

    public Document findAccessible(String id, Jwt jwt, String actorId) {
        return documentRepository.findAccessibleById(id, actorId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
    }

    public DocumentVersion currentVersion(Document document) {
        return versionRepository.findById(document.getCurrentVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Current document version not found"));
    }

    public String actorId(Jwt jwt) {
        UserIdentity identity = identityRepository
                .findByIssuerAndSubjectWithUser(jwt.getIssuer().toString(), jwt.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user is not provisioned"));
        return identity.getUser().getId();
    }

    public void audit(Document document, DocumentVersion version, String actorId, String action,
            Map<String, Object> details) {
        auditRepository.save(new DocumentAuditLog(document.getId(), version == null ? null : version.getId(),
                actorId, action, details));
    }

    public DocumentResponse response(Document document) {
        DocumentVersion version = document.getCurrentVersionId() == null ? null
                : versionRepository.findById(document.getCurrentVersionId()).orElse(null);
        return documentMapper.toResponse(document, version);
    }

    public DocumentVersionResponse versionResponse(DocumentVersion version) {
        return documentMapper.toVersionResponse(version);
    }

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A non-empty document file is required");
        }
    }

    public String safeFilename(MultipartFile file) {
        String filename = file.getOriginalFilename();
        return filename == null || filename.isBlank() ? "DOCUMENT" : Paths.get(filename).getFileName().toString();
    }

    public String contentType(MultipartFile file) {
        return file.getContentType() == null ? "application/octet-stream" : file.getContentType();
    }

    public boolean canEdit(Document document, String actorId) {
        return document.getOwnerId().equals(actorId)
                || permissionRepository.existsByDocumentIdAndSubjectTypeAndSubjectIdAndPermission(
                        document.getId(), DocumentConstants.USER_SUBJECT, actorId, DocumentConstants.EDIT_PERMISSION);
    }
}
