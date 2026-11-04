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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;

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
import com.qeema.util.storage.service.ObjectStorageGateway;
import com.qeema.util.document.constants.DocumentConstants;
import com.qeema.util.document.mapper.DocumentMapper;

@Service
@RequiredArgsConstructor
public class DocumentService implements DocumentUseCase {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final DocumentAuditLogRepository auditRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final UserIdentityRepository identityRepository;
    private final ObjectStorageGateway storage;
    private final AuditService auditService;
    private final DocumentMapper documentMapper;

    @Value("${app.documents.max-size-bytes:52428800}")
    private long maxFileSizeBytes;

    @Value("${app.documents.allowed-content-types:application/pdf,text/plain,image/png,image/jpeg}")
    private List<String> allowedContentTypes;

    @Transactional
    public DocumentResponse uploadDocument(DocumentUploadRequest request, MultipartFile file, Jwt jwt) {

        validateFile(file);

        String actorId = actorId(jwt);

        Document document = documentRepository
                .save(new Document(request.getTitle().trim(), request.getDescription(), actorId));

        addVersion(document, file, actorId);

        auditService.recordAuditEventWithSnapshots(new JwtAuthenticationToken(jwt), DocumentConstants.UPLOADED,
                DocumentConstants.ENTITY_TYPE,
                document.getId(),
                DocumentConstants.SUCCESS, Map.of(),
                Map.of("filename", getSafeFilename(file), "sizeBytes", file.getSize()),
                Map.of("filename", getSafeFilename(file)));

        auditDocument(document, null, actorId, DocumentConstants.UPLOAD, Map.of("filename", getSafeFilename(file)));

        return toResponse(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listDocuments(Jwt jwt) {

        List<DocumentResponse> documents = documentRepository.findAccessible(actorId(jwt)).stream()
                .map(this::toResponse).toList();

        auditService.recordAuditEventWithSnapshots(new JwtAuthenticationToken(jwt), DocumentConstants.LISTED,
                DocumentConstants.ENTITY_TYPE,
                DocumentConstants.ENTITY_ID,
                DocumentConstants.SUCCESS, Map.of(), Map.of("count", documents.size()),
                Map.of("count", documents.size()));

        return documents;
    }

    @Transactional
    public DocumentDownload downloadDocument(String id, Jwt jwt) {
        Document document = findAccessibleDocument(id, jwt);
        DocumentVersion version = findCurrentVersionForDocument(document);

        try {
            DocumentDownload download = new DocumentDownload(storage.download(version.getStorageKey()),
                    version.getOriginalFilename(), version.getContentType(), version.getSizeBytes());

            auditService.recordAuditEventWithSnapshots(new JwtAuthenticationToken(jwt), DocumentConstants.DOWNLOAD,
                    DocumentConstants.ENTITY_TYPE,
                    document.getId(), DocumentConstants.SUCCESS, Map.of(),
                    Map.of("version", version.getVersionNumber()),
                    Map.of("version", version.getVersionNumber()));

            return download;
        } catch (RuntimeException exception) {
            throw new DocumentStorageException("Unable to download document content", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentVersionResponse> listDocumentVersions(String id, Jwt jwt) {
        Document document = findAccessibleDocument(id, jwt);

        List<DocumentVersionResponse> versions = versionRepository
                .findByDocumentIdOrderByVersionNumberDesc(document.getId()).stream().map(this::toVersionResponse)
                .toList();

        auditService.recordAuditEventWithSnapshots(new JwtAuthenticationToken(jwt), DocumentConstants.VERSIONS_VIEWED,
                DocumentConstants.ENTITY_TYPE,
                document.getId(), DocumentConstants.SUCCESS, Map.of(), Map.of("count", versions.size()),
                Map.of("count", versions.size()));

        return versions;
    }

    @Transactional
    public DocumentVersionResponse uploadVersion(String id, MultipartFile file, Jwt jwt) {

        validateFile(file);

        Document document = findAccessibleDocument(id, jwt);

        String actorId = actorId(jwt);

        if (!document.getOwnerId().equals(actorId)
                && !permissionRepository.existsByDocumentIdAndSubjectTypeAndSubjectIdAndPermission(document.getId(),
                        DocumentConstants.USER_SUBJECT, actorId, DocumentConstants.EDIT_PERMISSION)) {
            throw new AccessDeniedException("You do not have permission to edit this document");
        }

        DocumentVersion version = addVersion(document, file, actorId);
        auditService.recordAuditEventWithSnapshots(new JwtAuthenticationToken(jwt), DocumentConstants.VERSION_CREATED,
                DocumentConstants.ENTITY_TYPE,
                document.getId(), DocumentConstants.SUCCESS, Map.of(), Map.of("version", version.getVersionNumber()),
                Map.of("version", version.getVersionNumber()));

        auditDocument(document, version, actorId, DocumentConstants.EDIT,
                Map.of("filename", getSafeFilename(file), "version", version.getVersionNumber()));

        return toVersionResponse(version);
    }

    @Transactional
    public void deleteDocument(String id, Jwt jwt) {
        Document document = findAccessibleDocument(id, jwt);

        String actorId = actorId(jwt);

        if (!document.getOwnerId().equals(actorId)) {
            throw new AccessDeniedException("Only the document owner can delete this document");
        }

        document.softDelete();

        documentRepository.save(document);

        auditDocument(document, null, actorId, DocumentConstants.DELETE, Map.of());

        auditService.recordAuditEventWithSnapshots(new JwtAuthenticationToken(jwt), DocumentConstants.DELETED,
                DocumentConstants.ENTITY_TYPE,
                document.getId(),
                DocumentConstants.SUCCESS, Map.of("status", "ACTIVE"), Map.of("status", "DELETED"), Map.of());
    }

    private DocumentVersion addVersion(Document document, MultipartFile file, String actorId) {
        int nextVersion = versionRepository.findTopByDocumentIdOrderByVersionNumberDesc(document.getId())
                .map(version -> version.getVersionNumber() + 1).orElse(1);

        String versionId = UUID.randomUUID().toString();

        String storageKey = DocumentConstants.DOCUMENT_KEY_PREFIX + document.getId()
                + DocumentConstants.VERSION_KEY_PREFIX + nextVersion + "/" + versionId;

        String checksum;

        try (InputStream input = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            DigestInputStream digestInput = new java.security.DigestInputStream(input, digest);

            storage.upload(storageKey, digestInput, file.getSize(), retrieveContentType(file));

            checksum = HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new DocumentStorageException("Unable to store document content", exception);
        } catch (RuntimeException exception) {
            throw new DocumentStorageException("Unable to store document content", exception);
        }

        DocumentVersion version = new DocumentVersion(document.getId(), nextVersion, storageKey, getSafeFilename(file),
                retrieveContentType(file), file.getSize(), checksum, actorId);

        version = versionRepository.save(version);

        document.setCurrentVersionId(version.getId());

        documentRepository.save(document);

        return version;
    }

    private Document findAccessibleDocument(String id, Jwt jwt) {
        return documentRepository.findAccessibleById(id, actorId(jwt))
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
    }

    private DocumentVersion findCurrentVersionForDocument(Document document) {
        return versionRepository.findById(document.getCurrentVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Current document version not found"));
    }

    private String actorId(Jwt jwt) {
        UserIdentity identity = identityRepository
                .findByIssuerAndSubjectWithUser(jwt.getIssuer().toString(), jwt.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user is not provisioned"));

        return identity.getUser().getId();
    }

    private void auditDocument(Document document, DocumentVersion version, String actorId, String action,
            Map<String, Object> details) {
        auditRepository.save(new DocumentAuditLog(document.getId(), version == null ? null : version.getId(), actorId,
                action, details));
    }

    private DocumentResponse toResponse(Document document) {
        DocumentVersion version = document.getCurrentVersionId() == null ? null
                : versionRepository.findById(document.getCurrentVersionId()).orElse(null);

        return documentMapper.toResponse(document, version);
    }

    private DocumentVersionResponse toVersionResponse(DocumentVersion version) {
        return documentMapper.toVersionResponse(version);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A non-empty document file is required");
        }
    }

    private String getSafeFilename(MultipartFile file) {

        final String DOCUMENT = "DOCUMENT";

        String filename = file.getOriginalFilename();

        if (filename == null || filename.isBlank())
            return DOCUMENT;

        return Paths.get(filename).getFileName().toString();
    }

    private String retrieveContentType(MultipartFile file) {
        final String CONTENT_TYPE = "application/octet-stream";

        return file.getContentType() == null ? CONTENT_TYPE : file.getContentType();
    }
}
