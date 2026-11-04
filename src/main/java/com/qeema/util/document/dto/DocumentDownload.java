package com.qeema.util.document.dto;

import java.io.InputStream;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDownload {
    private InputStream content;
    private String filename;
    private String contentType;
    private long sizeBytes;
}
