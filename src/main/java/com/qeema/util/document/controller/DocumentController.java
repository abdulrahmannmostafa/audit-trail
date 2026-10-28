package com.qeema.util.document.controller;

import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.qeema.util.document.dto.DocumentResponse;
import com.qeema.util.document.dto.DocumentDownload;
import com.qeema.util.document.dto.DocumentVersionResponse;
import com.qeema.util.document.dto.DocumentUploadRequest;
import com.qeema.util.document.service.DocumentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;

@RestController
@Validated
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Document upload, access, and version management")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document")
    public DocumentResponse upload(
            @RequestParam @NotBlank String title,
            @RequestParam(required = false) String description,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        return documentService.upload(
                new DocumentUploadRequest(title, description), file, jwt);
    }

    @GetMapping
    @Operation(summary = "List accessible documents")
    public List<DocumentResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return documentService.list(jwt);
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download the current document version")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        DocumentDownload download = documentService.download(id, jwt);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(download.filename())
                .build());
        headers.setContentType(MediaType.parseMediaType(download.contentType()));
        headers.setContentLength(download.sizeBytes());
        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(download.content()));
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "View document version history")
    public List<DocumentVersionResponse> versions(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        return documentService.versions(id, jwt);
    }

    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a new document version")
    public DocumentVersionResponse uploadVersion(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        return documentService.uploadVersion(id, file, jwt);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a document")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        documentService.delete(id, jwt);
        return ResponseEntity.noContent().build();
    }
}
