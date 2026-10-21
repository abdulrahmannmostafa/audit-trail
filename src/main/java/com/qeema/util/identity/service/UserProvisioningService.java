
package com.qeema.util.identity.service;

import com.qeema.util.identity.entity.User;
import com.qeema.util.identity.entity.UserIdentity;
import com.qeema.util.identity.dto.UserResponse;
import com.qeema.util.identity.repository.UserIdentityRepository;
import com.qeema.util.identity.repository.UserRepository;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Collection;

@Service
public class UserProvisioningService {

    private final UserRepository userRepository;
    private final UserIdentityRepository identityRepository;
    private final UserRoleService userRoleService;

    public UserProvisioningService(
            UserRepository userRepository,
            UserIdentityRepository identityRepository,
            UserRoleService userRoleService) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.userRoleService = userRoleService;
    }

    private List<String> extractKeycloakRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

        if (realmAccess == null) {
            return List.of();
        }

        Object rolesObject = realmAccess.get("roles");

        if (!(rolesObject instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                .filter(role -> !role.isBlank())
                .distinct()
                .toList();
    }

    @Transactional
    public UserResponse provision(Jwt jwt) {
        String issuer = jwt.getIssuer().toString();
        String subject = jwt.getSubject();

        Optional<UserIdentity> existingIdentity = identityRepository
                .findByIssuerAndSubjectWithUser(issuer, subject);

        if (existingIdentity.isPresent()) {
            return toResponse(existingIdentity.get().getUser());
        }

        return toResponse(createUser(jwt, issuer, subject));
    }

    private User createUser(
            Jwt jwt, String issuer, String subject) {

        String email = jwt.getClaimAsString("email");
        String displayName = jwt.getClaimAsString("name");

        if (email == null || email.isBlank()) {
            throw new IllegalStateException(
                    "The validated token has no email claim. "
                            + "Configure the email claim in Keycloak.");
        }

        if (displayName == null || displayName.isBlank()) {
            displayName = email;
        }

        User user = userRepository.saveAndFlush(
                new User(email, displayName, Optional.empty()));

        UserIdentity identity = new UserIdentity(
                user, "keycloak", issuer, subject);

        identityRepository.saveAndFlush(identity);

        List<String> keycloakRoles = extractKeycloakRoles(jwt);

        for (String role : keycloakRoles) {
            if (role.equals("USER") || role.equals("ADMIN")) {
                userRoleService.assignRole(user.getId(), role);
            }
        }

        return user;
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getStatus());
    }
}
