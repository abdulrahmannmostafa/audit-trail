package com.qeema.util.identity.utility;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.qeema.util.common.security.RoleNames;

@Component
public class IdentityUtility {

    public List<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(RoleNames::normalize)
                .filter(role -> !role.isBlank())
                .distinct()
                .toList();
    }

    public String getEmail(Jwt jwt) {
        return jwt.getClaimAsString("email");
    }

    public String displayName(Jwt jwt, String email) {
        String name = jwt.getClaimAsString("name");
        return name == null || name.isBlank() ? email : name;
    }
}
