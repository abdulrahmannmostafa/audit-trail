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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.qeema.util.common.exception.DocumentStorageException;
import com.qeema.util.audit.service.AuditService;
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

        private static final String DOCUMENT_TYPE_NAME = "DOCUMENT";

        private static final String DOCUMENT_DELETED = "DOCUMENT_DELETED";
        private static final String DOCUMENTS_LISTED = "DOCUMENTS_LISTED";
        private static final String DOCUMENT_UPLOADED = "DOCUMENT_UPLOADED";
        private static final String DOCUMENT_DOWNLOADED = "DOCUMENT_DOWNLOADED";
        private static final String DOCUMENT_VERSION_CREATED = "DOCUMENT_VERSION_CREATED";
        private static final String DOCUMENT_VERSIONS_VIEWED = "DOCUMENT_VERSIONS_VIEWED";

        private static final String DOCUMENT_SUCCESS = "SUCCESS";

        private static final String DOCUMENT_ENTITY_ID = "DOCUMENT";

        private static final String DOCUMENT_UPLOAD_ACTION = "UPLOAD";
        private static final String DOCUMENT_DOWNLOAD_ACTION = "DOWNLOAD";
        private static final String DOCUMENT_EDIT_ACTION = "EDIT";
        private static final String DOCUMENT_DELETE_ACTION = "DELETE";

        private final DocumentRepository documentRepository;
        private final DocumentVersionRepository versionRepository;
        private final DocumentAuditLogRepository auditRepository;
        private final DocumentPermissionRepository permissionRepository;
        private final UserIdentityRepository identityRepository;
        private final ObjectStorageService storage;
        private final AuditService auditService;
        private final long maxFileSizeBytes;
        private final List<String> allowedContentTypes;

        public DocumentService(
                        DocumentRepository documentRepository,
                        DocumentVersionRepository versionRepository,
                        DocumentAuditLogRepository auditRepository,
                        DocumentPermissionRepository permissionRepository,
                        UserIdentityRepository identityRepository,
                        ObjectStorageService storage,
                        AuditService auditService,
                        @Value("${app.documents.max-size-bytes:52428800}") long maxFileSizeBytes,
                        @Value("${app.documents.allowed-content-types}") List<String> allowedContentTypes) {
                this.documentRepository = documentRepository;
                this.versionRepository = versionRepository;
                this.auditRepository = auditRepository;
                this.permissionRepository = permissionRepository;
                this.identityRepository = identityRepository;
                this.storage = storage;
                this.auditService = auditService;
                this.maxFileSizeBytes = maxFileSizeBytes;
                this.allowedContentTypes = allowedContentTypes;
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
                auditService.record(
                                new JwtAuthenticationToken(jwt),
                                DOCUMENT_UPLOADED,
                                DOCUMENT_TYPE_NAME,
                                document.getId(),
                                DOCUMENT_SUCCESS,
                                Map.of(),
                                Map.of("filename", safeFilename(file), "sizeBytes", file.getSize()),
                                Map.of("filename", safeFilename(file)));
                audit(document, null, actorId, DOCUMENT_UPLOAD_ACTION,
                                Map.of("filename", safeFilename(file)));
                return toResponse(document);
        }

        @Transactional(readOnly = true)
        public List<DocumentResponse> list(Jwt jwt) {
                List<DocumentResponse> documents = documentRepository.findAccessible(actorId(jwt)).stream()
                                .map(this::toResponse)
                                .toList();
                auditService.record(new JwtAuthenticationToken(jwt),
                                DOCUMENTS_LISTED, DOCUMENT_TYPE_NAME, DOCUMENT_ENTITY_ID,
                                DOCUMENT_SUCCESS, Map.of(), Map.of("count", documents.size()),
                                Map.of("count", documents.size()));
                return documents;
        }

        @Transactional
        public DocumentDownload download(String id, Jwt jwt) {
                Document document = accessibleDocument(id, jwt);
                DocumentVersion version = currentVersion(document);
                try {
                        DocumentDownload download = new DocumentDownload(
                                        storage.download(version.getStorageKey()),
                                        version.getOriginalFilename(),
                                        version.getContentType(),
                                        version.getSizeBytes());
                        audit(document, version, actorId(jwt), DOCUMENT_DOWNLOAD_ACTION, Map.of());
                        auditService.record(
                                        new JwtAuthenticationToken(jwt),
                                        DOCUMENT_DOWNLOADED,
                                        DOCUMENT_TYPE_NAME,
                                        document.getId(),
                                        DOCUMENT_SUCCESS,
                                        Map.of(),
                                        Map.of("version", version.getVersionNumber()),
                                        Map.of("version", version.getVersionNumber()));
                        return download;
                } catch (RuntimeException exception) {
                        throw new DocumentStorageException(
                                        "Unable to download document content", exception);
                }
        }

        @Transactional(readOnly = true)
        public List<DocumentVersionResponse> versions(String id, Jwt jwt) {
                Document document = accessibleDocument(id, jwt);
                List<DocumentVersionResponse> versions = versionRepository
                                .findByDocumentIdOrderByVersionNumberDesc(document.getId())
                                .stream()
                                .map(this::toVersionResponse)
                                .toList();
                auditService.record(new JwtAuthenticationToken(jwt),
                                DOCUMENT_VERSIONS_VIEWED, DOCUMENT_TYPE_NAME,
                                document.getId(), DOCUMENT_SUCCESS,
                                Map.of(),
                                Map.of("count", versions.size()),
                                Map.of("count", versions.size()));
                return versions;
        }

        @Transactional
        public DocumentVersionResponse uploadVersion(
                        String id,
                        MultipartFile file,
                        Jwt jwt) {

                final String USER_SUBJECT = "USER";
                final String EDIT_PERMISSION = "EDIT";

                validateFile(file);
                Document document = accessibleDocument(id, jwt);
                String actorId = actorId(jwt);
                if (!document.getOwnerId().equals(actorId)
                                && !permissionRepository
                                                .existsByDocumentIdAndSubjectTypeAndSubjectIdAndPermission(
                                                                document.getId(), USER_SUBJECT, actorId,
                                                                EDIT_PERMISSION)) {
                        throw new AccessDeniedException(
                                        "You do not have permission to edit this document");
                }
                DocumentVersion version = addVersion(document, file, actorId);
                auditService.record(
                                new JwtAuthenticationToken(jwt),
                                DOCUMENT_VERSION_CREATED,
                                DOCUMENT_TYPE_NAME,
                                document.getId(),
                                DOCUMENT_SUCCESS,
                                Map.of(),
                                Map.of("version", version.getVersionNumber()),
                                Map.of("version", version.getVersionNumber()));
                audit(document, version, actorId, DOCUMENT_EDIT_ACTION,
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
                audit(document, null, actorId, DOCUMENT_DELETE_ACTION, Map.of());
                auditService.record(
                                new JwtAuthenticationToken(jwt),
                                DOCUMENT_DELETED,
                                DOCUMENT_TYPE_NAME,
                                document.getId(),
                                DOCUMENT_SUCCESS,
                                Map.of("status", "ACTIVE"),
                                Map.of("status", "DELETED"),
                                Map.of());
        }

        private DocumentVersion addVersion(
                        Document document,
                        MultipartFile file,
                        String actorId) {
                final String DOCUMENT_KEY_PREFIX = "documents/";
                final String VERSION_KEY_PREFIX = "/versions/";

                int nextVersion = versionRepository
                                .findTopByDocumentIdOrderByVersionNumberDesc(document.getId())
                                .map(version -> version.getVersionNumber() + 1)
                                .orElse(1);
                String versionId = UUID.randomUUID().toString();
                String storageKey = DOCUMENT_KEY_PREFIX + document.getId()
                                + VERSION_KEY_PREFIX + nextVersion + "/" + versionId;
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
                if (file.getSize() > maxFileSizeBytes) {
                        throw new IllegalArgumentException(
                                        "Document exceeds the maximum allowed size of "
                                                        + maxFileSizeBytes + " bytes");
                }

                String type = contentType(file);
                if (!allowedContentTypes.contains(type)) {
                        throw new IllegalArgumentException(
                                        "Unsupported document content type: " + type);
                }
        }

        private String safeFilename(MultipartFile file) {

                final String DOCUMENT = "DOCUMENT";
                String filename = file.getOriginalFilename();
                if (filename == null || filename.isBlank())
                        return DOCUMENT;
                return Paths.get(filename).getFileName().toString();
        }

        private String contentType(MultipartFile file) {
                final String CONTENT_TYPE = "application/octet-stream";

                String contentType = file.getContentType() == null
                                ? CONTENT_TYPE
                                : file.getContentType();
                return contentType.split(";", 2)[0].trim().toLowerCase(java.util.Locale.ROOT);
        }
}
