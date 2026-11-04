package com.qeema.util.common.api;

import java.time.Instant;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> fieldErrors = Map.of();

    public ApiError(int status, String error, String message, String path) {
        this(Instant.now(), status, error, message, path, Map.of());
    }
}
