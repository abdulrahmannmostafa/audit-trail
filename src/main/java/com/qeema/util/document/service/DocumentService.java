package com.qeema.util.document.service;

import java.util.List;
import java.util.Map;

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
import com.qeema.util.document.dto.DocumentDownload;
import com.qeema.util.document.dto.DocumentResponse;
import com.qeema.util.document.dto.DocumentUploadRequest;
import com.qeema.util.document.dto.DocumentVersionResponse;
import com.qeema.util.document.entity.Document;
import com.qeema.util.document.entity.DocumentVersion;
import com.qeema.util.document.repository.DocumentRepository;
import com.qeema.util.document.repository.DocumentVersionRepository;
import com.qeema.util.storage.service.ObjectStorageGateway;
import com.qeema.util.document.constants.DocumentConstants;
import com.qeema.util.document.utility.DocumentUtility;

@Service
@RequiredArgsConstructor
public class DocumentService implements DocumentUseCase {

        private final DocumentRepository documentRepository;
        private final DocumentVersionRepository versionRepository;
        private final ObjectStorageGateway storage;
        private final AuditService auditService;
        private final DocumentUtility documentUtility;

        @Value("${app.documents.max-size-bytes:52428800}")
        private long maxFileSizeBytes;

        @Value("${app.documents.allowed-content-types:application/pdf,text/plain,image/png,image/jpeg}")
        private List<String> allowedContentTypes;

        @Transactional
        public DocumentResponse uploadDocument(DocumentUploadRequest request, MultipartFile file, Jwt jwt) {

                documentUtility.validateFile(file);

                String actorId = documentUtility.actorId(jwt);

                Document document = documentRepository
                                .save(new Document(request.getTitle().trim(), request.getDescription(), actorId));

                documentUtility.addVersion(document, file, actorId, allowedContentTypes);

                auditService.recordAuditEventWithSnapshots(new JwtAuthenticationToken(jwt), DocumentConstants.UPLOADED,
                                DocumentConstants.ENTITY_TYPE,
                                document.getId(),
                                DocumentConstants.SUCCESS, Map.of(),
                                Map.of("filename", documentUtility.safeFilename(file), "sizeBytes", file.getSize()),
                                Map.of("filename", documentUtility.safeFilename(file)));

                documentUtility.audit(document, null, actorId, DocumentConstants.UPLOAD,
                                Map.of("filename", documentUtility.safeFilename(file)));

                return documentUtility.response(document);
        }

        @Transactional(readOnly = true)
        public List<DocumentResponse> listDocuments(Jwt jwt) {

                List<DocumentResponse> documents = documentRepository.findAccessible(documentUtility.actorId(jwt))
                                .stream()
                                .map(documentUtility::response).toList();

                auditService.recordAuditEventWithSnapshots(new JwtAuthenticationToken(jwt), DocumentConstants.LISTED,
                                DocumentConstants.ENTITY_TYPE,
                                DocumentConstants.ENTITY_ID,
                                DocumentConstants.SUCCESS, Map.of(), Map.of("count", documents.size()),
                                Map.of("count", documents.size()));

                return documents;
        }

        @Transactional
        public DocumentDownload downloadDocument(String id, Jwt jwt) {
                Document document = documentUtility.findAccessible(id, jwt, documentUtility.actorId(jwt));
                DocumentVersion version = documentUtility.currentVersion(document);

                try {
                        DocumentDownload download = new DocumentDownload(storage.download(version.getStorageKey()),
                                        version.getOriginalFilename(), version.getContentType(),
                                        version.getSizeBytes());

                        auditService.recordAuditEventWithSnapshots(new JwtAuthenticationToken(jwt),
                                        DocumentConstants.DOWNLOAD,
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
                Document document = documentUtility.findAccessible(id, jwt, documentUtility.actorId(jwt));

                List<DocumentVersionResponse> versions = versionRepository
                                .findByDocumentIdOrderByVersionNumberDesc(document.getId()).stream()
                                .map(documentUtility::versionResponse)
                                .toList();

                auditService.recordAuditEventWithSnapshots(new JwtAuthenticationToken(jwt),
                                DocumentConstants.VERSIONS_VIEWED,
                                DocumentConstants.ENTITY_TYPE,
                                document.getId(), DocumentConstants.SUCCESS, Map.of(), Map.of("count", versions.size()),
                                Map.of("count", versions.size()));

                return versions;
        }

        @Transactional
        public DocumentVersionResponse uploadVersion(String id, MultipartFile file, Jwt jwt) {

                documentUtility.validateFile(file);

                Document document = documentUtility.findAccessible(id, jwt, documentUtility.actorId(jwt));

                String actorId = documentUtility.actorId(jwt);

                if (!documentUtility.canEdit(document, actorId)) {
                        throw new AccessDeniedException("You do not have permission to edit this document");
                }

                DocumentVersion version = documentUtility.addVersion(document, file, actorId, allowedContentTypes);
                auditService.recordAuditEventWithSnapshots(new JwtAuthenticationToken(jwt),
                                DocumentConstants.VERSION_CREATED,
                                DocumentConstants.ENTITY_TYPE,
                                document.getId(), DocumentConstants.SUCCESS, Map.of(),
                                Map.of("version", version.getVersionNumber()),
                                Map.of("version", version.getVersionNumber()));

                documentUtility.audit(document, version, actorId, DocumentConstants.EDIT,
                                Map.of("filename", documentUtility.safeFilename(file), "version",
                                                version.getVersionNumber()));

                return documentUtility.versionResponse(version);
        }

        @Transactional
        public void deleteDocument(String id, Jwt jwt) {
                Document document = documentUtility.findAccessible(id, jwt, documentUtility.actorId(jwt));

                String actorId = documentUtility.actorId(jwt);

                if (!document.getOwnerId().equals(actorId)) {
                        throw new AccessDeniedException("Only the document owner can delete this document");
                }

                document.softDelete();

                documentRepository.save(document);

                documentUtility.audit(document, null, actorId, DocumentConstants.DELETE, Map.of());

                auditService.recordAuditEventWithSnapshots(new JwtAuthenticationToken(jwt), DocumentConstants.DELETED,
                                DocumentConstants.ENTITY_TYPE,
                                document.getId(),
                                DocumentConstants.SUCCESS, Map.of("status", "ACTIVE"), Map.of("status", "DELETED"),
                                Map.of());
        }

}
