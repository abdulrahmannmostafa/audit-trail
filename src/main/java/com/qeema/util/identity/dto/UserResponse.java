package com.qeema.util.identity.dto;

public record UserResponse(
        String id,
        String email,
        String displayName,
        String status) {
}
