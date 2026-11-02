package com.qeema.util.document.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    private String id;
    private String title;
    private String description;
    private String ownerId;
    private String currentVersionId;
    private String filename;
    private String contentType;
    private long sizeBytes;
    private Instant createdAt;
    private Instant updatedAt;
}
