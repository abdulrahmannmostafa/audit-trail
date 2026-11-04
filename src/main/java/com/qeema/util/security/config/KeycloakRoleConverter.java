package com.qeema.util.security.config;

import java.util.Collection;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.qeema.util.common.exception.InvalidIdentityTokenException;
import com.qeema.util.identity.dto.UserResponse;
import com.qeema.util.identity.service.UserProvisioningService;
import com.qeema.util.identity.service.UserRoleService;
import com.qeema.util.common.security.RoleNames;
import com.qeema.util.security.constants.SecurityConstants;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final UserProvisioningService provisioningService;
    private final UserRoleService userRoleService;

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {

        // Find or create the local application user
        UserResponse user;
        try {
            user = provisioningService.provision(new JwtAuthenticationToken(jwt));
        } catch (InvalidIdentityTokenException exception) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_token", "The access token is missing required identity claims", null),
                    exception);
        }

        // Do not authorize disabled application accounts
        if (!SecurityConstants.ACTIVE_STATUS.equalsIgnoreCase(user.getStatus())) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_token", "This application account is disabled", null));
        }

        // Load roles from MySQL, not from Keycloak's role claims
        List<String> roleNames = userRoleService.getRoleNames(user.getId());

        return roleNames.stream().map(RoleNames::normalize).distinct()
                .map(name -> (GrantedAuthority) new SimpleGrantedAuthority(SecurityConstants.ROLE_PREFIX + name))
                .toList();
    }
}
