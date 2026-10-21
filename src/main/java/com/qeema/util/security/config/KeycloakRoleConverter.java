package com.qeema.util.security.config;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.qeema.util.common.exception.InvalidIdentityTokenException;
import com.qeema.util.identity.dto.UserResponse;
import com.qeema.util.identity.service.UserProvisioningService;
import com.qeema.util.identity.service.UserRoleService;

@Component
public class KeycloakRoleConverter
        implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final UserProvisioningService provisioningService;
    private final UserRoleService userRoleService;

    public KeycloakRoleConverter(
            UserProvisioningService provisioningService,
            UserRoleService userRoleService) {
        this.provisioningService = provisioningService;
        this.userRoleService = userRoleService;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {

        // Find or create the local application user
        UserResponse user;
        try {
            user = provisioningService.provision(jwt);
        } catch (InvalidIdentityTokenException exception) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(
                            "invalid_token",
                            "The access token is missing required identity claims.",
                            null),
                    exception);
        }

        // Do not authorize disabled application accounts
        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(
                            "invalid_token",
                            "This application account is disabled.",
                            null));
        }

        // Load roles from MySQL, not from Keycloak's role claims
        List<String> roleNames = userRoleService.getRoleNames(user.id());

        return roleNames.stream()
                .map(name -> name.toUpperCase(Locale.ROOT))
                .distinct()
                .map(name -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + name))
                .toList();
    }
}
