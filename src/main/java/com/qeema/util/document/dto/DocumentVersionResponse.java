package com.qeema.util.document.dto;

import java.time.Instant;

public record DocumentVersionResponse(
                String id,
                int versionNumber,
                String originalFilename,
                String contentType,
                long sizeBytes,
                String checksumSha256,
                String uploadedBy,
                Instant createdAt) {
}
