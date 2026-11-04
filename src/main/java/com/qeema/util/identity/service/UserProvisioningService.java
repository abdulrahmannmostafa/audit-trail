package com.qeema.util.identity.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import com.qeema.util.common.exception.InvalidIdentityTokenException;
import com.qeema.util.audit.service.AuditService;
import com.qeema.util.identity.dto.UserResponse;
import com.qeema.util.identity.entity.User;
import com.qeema.util.identity.entity.UserIdentity;
import com.qeema.util.identity.repository.UserIdentityRepository;
import com.qeema.util.identity.repository.UserRepository;
import com.qeema.util.common.security.RoleNames;
import com.qeema.util.identity.constants.IdentityConstants;
import com.qeema.util.identity.mapper.UserMapper;

@Service
@RequiredArgsConstructor
public class UserProvisioningService {

    private final UserRepository userRepository;
    private final UserIdentityRepository identityRepository;
    private final UserRoleService userRoleService;
    private final AuditService auditService;
    private final UserMapper userMapper;

    private List<String> extractKeycloakRoles(Jwt jwt) {
        final String REALM_ACCESS_NAME = "realm_access";
        final String ROLES_NAME = "roles";

        Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_NAME);

        if (realmAccess == null) {
            return List.of();
        }

        Object rolesObject = realmAccess.get(ROLES_NAME);

        if (!(rolesObject instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles.stream().filter(String.class::isInstance).map(String.class::cast).map(RoleNames::normalize)
                .filter(role -> !role.isBlank()).distinct().toList();
    }

    @Transactional
    public UserResponse provision(Jwt jwt) {
        String issuer = jwt.getIssuer().toString();
        String subject = jwt.getSubject();

        Optional<UserIdentity> existingIdentity = identityRepository.findByIssuerAndSubjectWithUser(issuer, subject);

        if (existingIdentity.isPresent()) {
            auditService.recordAuditEvent(new JwtAuthenticationToken(jwt), "USER_VIEWED",
                    IdentityConstants.USER_ENTITY,
                    existingIdentity.get().getUser().getId(), IdentityConstants.SUCCESS,
                    Map.of("operation", "current-user"));

            return toResponse(existingIdentity.get().getUser());
        }

        return toResponse(createUser(jwt, issuer, subject));
    }

    private User createUser(Jwt jwt, String issuer, String subject) {

        final String EMAIL_CLAIM_NAME = "email";
        final String NAME_CLAIM_NAME = "name";

        String email = jwt.getClaimAsString(EMAIL_CLAIM_NAME);
        String displayName = jwt.getClaimAsString(NAME_CLAIM_NAME);

        if (email == null || email.isBlank()) {
            throw new InvalidIdentityTokenException(
                    "The validated token has no email claim. " + "Configure the email claim in Keycloak");
        }

        if (displayName == null || displayName.isBlank()) {
            displayName = email;
        }

        User user = userRepository.saveAndFlush(new User(email, displayName, Optional.empty()));

        UserIdentity identity = new UserIdentity(user, IdentityConstants.KEYCLOAK, issuer, subject);

        identityRepository.saveAndFlush(identity);

        List<String> keycloakRoles = extractKeycloakRoles(jwt);

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);

        for (String role : keycloakRoles) {
            if (role.equals(IdentityConstants.USER) || role.equals(IdentityConstants.ADMIN)) {
                userRoleService.assignRole(user.getId(), role, authentication);
            }
        }

        auditService.recordAuditEventWithSnapshots(authentication, "USER_CREATED", IdentityConstants.USER_ENTITY,
                user.getId(),
                IdentityConstants.SUCCESS, Map.of(), Map.of("displayName", user.getDisplayName()),
                Map.of("identityProvider", IdentityConstants.KEYCLOAK));

        return user;
    }

    private UserResponse toResponse(User user) {
        return userMapper.toResponse(user);
    }
}
