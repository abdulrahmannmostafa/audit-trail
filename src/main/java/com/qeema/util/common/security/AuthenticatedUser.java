package com.qeema.util.common.security;

import java.util.Map;
import java.util.Set;

public record AuthenticatedUser(
        String subject,
        String issuer,
        String email,
        String displayName,
        Set<String> roles,
        Map<String, Object> claims) {

    public AuthenticatedUser {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        claims = claims == null ? Map.of() : Map.copyOf(claims);
    }
}
