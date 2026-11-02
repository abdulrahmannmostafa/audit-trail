package com.qeema.util.document.service;

import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;

import com.qeema.util.document.dto.DocumentDownload;
import com.qeema.util.document.dto.DocumentResponse;
import com.qeema.util.document.dto.DocumentUploadRequest;
import com.qeema.util.document.dto.DocumentVersionResponse;

public interface DocumentUseCase {
    DocumentResponse upload(DocumentUploadRequest request, MultipartFile file, Jwt jwt);

    List<DocumentResponse> list(Jwt jwt);

    DocumentDownload download(String id, Jwt jwt);

    List<DocumentVersionResponse> versions(String id, Jwt jwt);

    DocumentVersionResponse uploadVersion(String id, MultipartFile file, Jwt jwt);

    void delete(String id, Jwt jwt);
}
