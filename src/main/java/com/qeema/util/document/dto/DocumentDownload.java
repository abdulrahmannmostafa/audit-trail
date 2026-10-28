package com.qeema.util.document.dto;

import java.io.InputStream;

public record DocumentDownload(
                InputStream content,
                String filename,
                String contentType,
                long sizeBytes) {
}
