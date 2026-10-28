package com.qeema.util.document.dto;

import java.time.Instant;

public record DocumentResponse(
                String id,
                String title,
                String description,
                String ownerId,
                String currentVersionId,
                String filename,
                String contentType,
                long sizeBytes,
                Instant createdAt,
                Instant updatedAt) {
}
