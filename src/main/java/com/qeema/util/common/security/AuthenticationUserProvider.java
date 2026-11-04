package com.qeema.util.common.security;

import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationUserProvider {

    public AuthenticatedUser resolve(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return fromJwt(jwtAuthentication.getToken(), jwtAuthentication);
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("An authenticated user is required");
        }

        return new AuthenticatedUser(
                authentication.getName(),
                null,
                authentication.getName(),
                authentication.getName(),
                roles(authentication),
                Map.of());
    }

    private AuthenticatedUser fromJwt(Jwt jwt, Authentication authentication) {
        String subject = required(jwt.getSubject(), "Authenticated token has no subject");
        String email = jwt.getClaimAsString("email");
        String displayName = valueOrDefault(jwt.getClaimAsString("name"), email);
        Set<String> tokenRoles = new HashSet<>(roles(authentication));
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (realmAccess instanceof Map<?, ?> access && access.get("roles") instanceof Collection<?> claimsRoles) {
            claimsRoles.stream().filter(String.class::isInstance).map(String.class::cast).forEach(tokenRoles::add);
        }
        return new AuthenticatedUser(subject, jwt.getIssuer() == null ? null : jwt.getIssuer().toString(),
                email, displayName, tokenRoles, jwt.getClaims());
    }

    private Set<String> roles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toUnmodifiableSet());
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String required(String value, String message) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }
}
