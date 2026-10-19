package com.qeema.util.identity.service;

import com.qeema.util.identity.entity.User;
import com.qeema.util.identity.entity.UserIdentity;
import com.qeema.util.identity.repository.UserIdentityRepository;
import com.qeema.util.identity.repository.UserRepository;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserProvisioningService {

    private final UserRepository userRepository;
    private final UserIdentityRepository identityRepository;

    public UserProvisioningService(
            UserRepository userRepository,
            UserIdentityRepository identityRepository) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
    }

    // Load the user in the transactional context to avoid lazy exception when
    // accessing the user entity later
    @Transactional(readOnly = true)
    public User provision(Jwt jwt) {

        String issuer = jwt.getIssuer().toString();
        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String displayName = jwt.getClaimAsString("name");

        return identityRepository
                .findByIssuerAndSubjectWithUser(issuer, subject)
                .map(identity -> identity.getUser())
                .orElseGet(() -> createUser(issuer, subject, "keycloak", email, displayName));
    }

    private User createUser(String issuer, String subject, String provider, String email, String displayName) {

        User user = userRepository.saveAndFlush(
                new User(email, displayName, Optional.empty()));

        UserIdentity identity = new UserIdentity(user, provider, issuer, subject);

        identityRepository.save(identity);

        return user;
    }
}