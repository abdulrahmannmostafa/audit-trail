package com.qeema.util.document.controller;

import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
import com.qeema.util.document.service.DocumentUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;

@RestController
@Validated
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Document upload, access, and version management")
public class DocumentController {

    private final DocumentUseCase documentService;

    public DocumentController(DocumentUseCase documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document")
    public DocumentResponse upload(@RequestParam @NotBlank String title,
            @RequestParam(required = false) String description, @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return documentService.uploadDocument(new DocumentUploadRequest(title, description), file, authentication);
    }

    @GetMapping
    @Operation(summary = "List accessible documents")
    public List<DocumentResponse> list(Authentication authentication) {
        return documentService.listDocuments(authentication);
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download the current document version")
    public ResponseEntity<InputStreamResource> download(@PathVariable String id, Authentication authentication) {

        DocumentDownload download = documentService.downloadDocument(id, authentication);

        HttpHeaders headers = new HttpHeaders();

        // Set the Content-Disposition header to indicate that the response is an
        // attachment with the specified filename
        headers.setContentDisposition(ContentDisposition.attachment().filename(download.getFilename()).build());

        // Set the Content-Type and Content-Length headers based on the document's
        // properties
        headers.setContentType(MediaType.parseMediaType(download.getContentType()));
        headers.setContentLength(download.getSizeBytes());

        return ResponseEntity.ok().headers(headers).body(new InputStreamResource(download.getContent()));
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "View document version history")
    public List<DocumentVersionResponse> versions(@PathVariable String id, Authentication authentication) {
        return documentService.listDocumentVersions(id, authentication);
    }

    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a new document version")
    public DocumentVersionResponse uploadVersion(@PathVariable String id, @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return documentService.uploadVersion(id, file, authentication);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a document")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication authentication) {
        documentService.deleteDocument(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
