package com.qeema.util.document.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVersionResponse {
    private String id;
    private int versionNumber;
    private String originalFilename;
    private String contentType;
    private long sizeBytes;
    private String checksumSha256;
    private String uploadedBy;
    private Instant createdAt;
}
