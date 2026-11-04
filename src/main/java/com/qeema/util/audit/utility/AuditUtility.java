package com.qeema.util.audit.utility;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class AuditUtility {

    public String[] actorIds(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            String actorId = jwtAuthentication.getToken().getSubject();
            if (actorId == null || actorId.isBlank()) {
                throw new IllegalArgumentException("Authenticated JWT has no subject claim");
            }

            String username = jwtAuthentication.getToken().getClaimAsString("preferred_username");
            return new String[] { actorId, username == null || username.isBlank() ? actorId : username };
        }

        if (authentication != null && authentication.isAuthenticated()
                && authentication.getName() != null && !authentication.getName().isBlank()) {
            return new String[] { authentication.getName(), authentication.getName() };
        }

        return new String[] { "system", "system" };
    }

    public String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
