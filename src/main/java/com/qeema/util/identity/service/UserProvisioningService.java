package com.qeema.util.identity.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
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
import com.qeema.util.identity.constants.IdentityConstants;
import com.qeema.util.identity.mapper.UserMapper;
import com.qeema.util.identity.utility.IdentityUtility;
import com.qeema.util.common.security.AuthenticatedUser;
import com.qeema.util.common.security.AuthenticationUserProvider;

@Service
@RequiredArgsConstructor
public class UserProvisioningService {

    private final UserRepository userRepository;
    private final UserIdentityRepository identityRepository;
    private final UserRoleService userRoleService;
    private final AuditService auditService;
    private final UserMapper userMapper;
    private final IdentityUtility identityUtility;
    private final AuthenticationUserProvider authenticationUserProvider;

    @Transactional
    public UserResponse provision(Authentication authentication) {
        AuthenticatedUser userIdentity = authenticationUserProvider.resolve(authentication);
        String issuer = userIdentity.issuer();
        String subject = userIdentity.subject();

        Optional<UserIdentity> existingIdentity = identityRepository.findByIssuerAndSubjectWithUser(issuer, subject);

        if (existingIdentity.isPresent()) {
            auditService.recordAuditEvent(authentication, "USER_VIEWED",
                    IdentityConstants.USER_ENTITY,
                    existingIdentity.get().getUser().getId(), IdentityConstants.SUCCESS,
                    Map.of("operation", "current-user"));

            return userMapper.toResponse(existingIdentity.get().getUser());
        }

        String email = userIdentity.email();

        if (email == null || email.isBlank()) {
            throw new InvalidIdentityTokenException(
                    "The validated token has no email claim. " + "Configure the email claim in Keycloak");
        }

        String displayName = userIdentity.displayName();

        User user = userRepository.saveAndFlush(new User(email, displayName, Optional.empty()));

        UserIdentity identity = new UserIdentity(user, IdentityConstants.KEYCLOAK, issuer, subject);

        identityRepository.saveAndFlush(identity);

        List<String> keycloakRoles = identityUtility.extractRoles(userIdentity);

        for (String role : keycloakRoles) {
            if (role.equals(IdentityConstants.USER) || role.equals(IdentityConstants.ADMIN)) {
                userRoleService.assignRole(user.getId(), role, authentication);
            }
        }

        auditService.recordAuditEventWithSnapshots(authentication, "USER_CREATED", IdentityConstants.USER_ENTITY,
                user.getId(),
                IdentityConstants.SUCCESS, Map.of(), Map.of("displayName", user.getDisplayName()),
                Map.of("identityProvider", IdentityConstants.KEYCLOAK));

        return userMapper.toResponse(user);
    }
}
