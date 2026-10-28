package com.qeema.util.document.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.DigestInputStream;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.qeema.util.common.exception.DocumentStorageException;
import org.springframework.security.access.AccessDeniedException;
import com.qeema.util.common.exception.ResourceNotFoundException;
import com.qeema.util.document.dto.DocumentDownload;
import com.qeema.util.document.dto.DocumentResponse;
import com.qeema.util.document.dto.DocumentUploadRequest;
import com.qeema.util.document.dto.DocumentVersionResponse;
import com.qeema.util.document.entity.Document;
import com.qeema.util.document.entity.DocumentAuditLog;
import com.qeema.util.document.entity.DocumentVersion;
import com.qeema.util.document.repository.DocumentAuditLogRepository;
import com.qeema.util.document.repository.DocumentRepository;
import com.qeema.util.document.repository.DocumentPermissionRepository;
import com.qeema.util.document.repository.DocumentVersionRepository;
import com.qeema.util.identity.entity.UserIdentity;
import com.qeema.util.identity.repository.UserIdentityRepository;
import com.qeema.util.storage.service.ObjectStorageService;

@Service
public class DocumentService {

        private final DocumentRepository documentRepository;
        private final DocumentVersionRepository versionRepository;
        private final DocumentAuditLogRepository auditRepository;
        private final DocumentPermissionRepository permissionRepository;
        private final UserIdentityRepository identityRepository;
        private final ObjectStorageService storage;

        public DocumentService(
                        DocumentRepository documentRepository,
                        DocumentVersionRepository versionRepository,
                        DocumentAuditLogRepository auditRepository,
                        DocumentPermissionRepository permissionRepository,
                        UserIdentityRepository identityRepository,
                        ObjectStorageService storage) {
                this.documentRepository = documentRepository;
                this.versionRepository = versionRepository;
                this.auditRepository = auditRepository;
                this.permissionRepository = permissionRepository;
                this.identityRepository = identityRepository;
                this.storage = storage;
        }

        @Transactional
        public DocumentResponse upload(
                        DocumentUploadRequest request,
                        MultipartFile file,
                        Jwt jwt) {
                validateFile(file);
                String actorId = actorId(jwt);
                Document document = documentRepository.save(new Document(
                                request.title().trim(),
                                request.description(),
                                actorId));

                addVersion(document, file, actorId);
                audit(document, null, actorId, "UPLOAD",
                                Map.of("filename", safeFilename(file)));
                return toResponse(document);
        }

        @Transactional(readOnly = true)
        public List<DocumentResponse> list(Jwt jwt) {
                return documentRepository.findAccessible(actorId(jwt)).stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional
        public DocumentDownload download(String id, Jwt jwt) {
                Document document = accessibleDocument(id, jwt);
                DocumentVersion version = currentVersion(document);
                audit(document, version, actorId(jwt), "DOWNLOAD", Map.of());
                try {
                        return new DocumentDownload(
                                        storage.download(version.getStorageKey()),
                                        version.getOriginalFilename(),
                                        version.getContentType(),
                                        version.getSizeBytes());
                } catch (RuntimeException exception) {
                        throw new DocumentStorageException(
                                        "Unable to download document content", exception);
                }
        }

        @Transactional(readOnly = true)
        public List<DocumentVersionResponse> versions(String id, Jwt jwt) {
                Document document = accessibleDocument(id, jwt);
                return versionRepository
                                .findByDocumentIdOrderByVersionNumberDesc(document.getId())
                                .stream()
                                .map(this::toVersionResponse)
                                .toList();
        }

        @Transactional
        public DocumentVersionResponse uploadVersion(
                        String id,
                        MultipartFile file,
                        Jwt jwt) {
                validateFile(file);
                Document document = accessibleDocument(id, jwt);
                String actorId = actorId(jwt);
                if (!document.getOwnerId().equals(actorId)
                                && !permissionRepository
                                                .existsByDocumentIdAndSubjectTypeAndSubjectIdAndPermission(
                                                                document.getId(), "USER", actorId, "EDIT")) {
                        throw new AccessDeniedException(
                                        "You do not have permission to edit this document");
                }
                DocumentVersion version = addVersion(document, file, actorId);
                audit(document, version, actorId, "EDIT",
                                Map.of("filename", safeFilename(file),
                                                "version", version.getVersionNumber()));
                return toVersionResponse(version);
        }

        @Transactional
        public void delete(String id, Jwt jwt) {
                Document document = accessibleDocument(id, jwt);
                String actorId = actorId(jwt);
                if (!document.getOwnerId().equals(actorId)) {
                        throw new AccessDeniedException(
                                        "Only the document owner can delete this document");
                }
                document.softDelete();
                documentRepository.save(document);
                audit(document, null, actorId, "DELETE", Map.of());
        }

        private DocumentVersion addVersion(
                        Document document,
                        MultipartFile file,
                        String actorId) {
                int nextVersion = versionRepository
                                .findTopByDocumentIdOrderByVersionNumberDesc(document.getId())
                                .map(version -> version.getVersionNumber() + 1)
                                .orElse(1);
                String versionId = UUID.randomUUID().toString();
                String storageKey = "documents/" + document.getId()
                                + "/versions/" + nextVersion + "/" + versionId;
                String checksum;

                try (InputStream input = file.getInputStream()) {
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        DigestInputStream digestInput = new java.security.DigestInputStream(input,
                                        digest);
                        storage.upload(
                                        storageKey,
                                        digestInput,
                                        file.getSize(),
                                        contentType(file));
                        checksum = HexFormat.of().formatHex(digest.digest());
                } catch (IOException | NoSuchAlgorithmException exception) {
                        throw new DocumentStorageException(
                                        "Unable to store document content", exception);
                } catch (RuntimeException exception) {
                        throw new DocumentStorageException(
                                        "Unable to store document content", exception);
                }

                DocumentVersion version = new DocumentVersion(
                                document.getId(),
                                nextVersion,
                                storageKey,
                                safeFilename(file),
                                contentType(file),
                                file.getSize(),
                                checksum,
                                actorId);
                version = versionRepository.save(version);
                document.setCurrentVersionId(version.getId());
                documentRepository.save(document);
                return version;
        }

        private Document accessibleDocument(String id, Jwt jwt) {
                return documentRepository.findAccessibleById(id, actorId(jwt))
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Document not found"));
        }

        private DocumentVersion currentVersion(Document document) {
                return versionRepository.findById(document.getCurrentVersionId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Current document version not found"));
        }

        private String actorId(Jwt jwt) {
                UserIdentity identity = identityRepository
                                .findByIssuerAndSubjectWithUser(
                                                jwt.getIssuer().toString(), jwt.getSubject())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Authenticated user is not provisioned"));
                return identity.getUser().getId();
        }

        private void audit(
                        Document document,
                        DocumentVersion version,
                        String actorId,
                        String action,
                        Map<String, Object> details) {
                auditRepository.save(new DocumentAuditLog(
                                document.getId(),
                                version == null ? null : version.getId(),
                                actorId,
                                action,
                                details));
        }

        private DocumentResponse toResponse(Document document) {
                DocumentVersion version = document.getCurrentVersionId() == null
                                ? null
                                : versionRepository.findById(document.getCurrentVersionId())
                                                .orElse(null);
                return new DocumentResponse(
                                document.getId(),
                                document.getTitle(),
                                document.getDescription(),
                                document.getOwnerId(),
                                document.getCurrentVersionId(),
                                version == null ? null : version.getOriginalFilename(),
                                version == null ? null : version.getContentType(),
                                version == null ? 0 : version.getSizeBytes(),
                                document.getCreatedAt(),
                                document.getUpdatedAt());
        }

        private DocumentVersionResponse toVersionResponse(DocumentVersion version) {
                return new DocumentVersionResponse(
                                version.getId(),
                                version.getVersionNumber(),
                                version.getOriginalFilename(),
                                version.getContentType(),
                                version.getSizeBytes(),
                                version.getChecksumSha256(),
                                version.getUploadedBy(),
                                version.getCreatedAt());
        }

        private void validateFile(MultipartFile file) {
                if (file == null || file.isEmpty()) {
                        throw new IllegalArgumentException("A non-empty document file is required");
                }
        }

        private String safeFilename(MultipartFile file) {
                String filename = file.getOriginalFilename();
                if (filename == null || filename.isBlank())
                        return "document";
                return Paths.get(filename).getFileName().toString();
        }

        private String contentType(MultipartFile file) {
                return file.getContentType() == null
                                ? "application/octet-stream"
                                : file.getContentType();
        }
}
