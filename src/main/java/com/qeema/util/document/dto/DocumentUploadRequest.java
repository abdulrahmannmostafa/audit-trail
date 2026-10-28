package com.qeema.util.document.dto;

import jakarta.validation.constraints.NotBlank;

public record DocumentUploadRequest(
                @NotBlank(message = "Title must not be blank") String title,
                String description) {
}
