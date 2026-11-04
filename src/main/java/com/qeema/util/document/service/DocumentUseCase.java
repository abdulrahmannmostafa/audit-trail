package com.qeema.util.document.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import org.springframework.security.core.Authentication;
import com.qeema.util.document.dto.DocumentDownload;
import com.qeema.util.document.dto.DocumentResponse;
import com.qeema.util.document.dto.DocumentUploadRequest;
import com.qeema.util.document.dto.DocumentVersionResponse;

public interface DocumentUseCase {
    DocumentResponse uploadDocument(DocumentUploadRequest request, MultipartFile file, Authentication authentication);

    List<DocumentResponse> listDocuments(Authentication authentication);

    DocumentDownload downloadDocument(String id, Authentication authentication);

    List<DocumentVersionResponse> listDocumentVersions(String id, Authentication authentication);

    DocumentVersionResponse uploadVersion(String id, MultipartFile file, Authentication authentication);

    void deleteDocument(String id, Authentication authentication);
}
