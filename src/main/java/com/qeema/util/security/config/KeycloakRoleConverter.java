package com.qeema.util.security.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class KeycloakRoleConverter
        implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

        if (realmAccess == null) {
            return authorities;
        }

        Object rolesObject = realmAccess.get("roles");

        if (rolesObject instanceof List<?> roles) {
            for (Object role : roles) {
                if (role instanceof String roleName) {
                    authorities.add(
                            new SimpleGrantedAuthority("ROLE_" + roleName));
                }
            }
        }

        return authorities;
    }
}
