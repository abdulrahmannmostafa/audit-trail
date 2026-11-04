package com.qeema.util.identity.utility;

import java.util.List;
import org.springframework.stereotype.Component;

import com.qeema.util.common.security.RoleNames;
import com.qeema.util.common.security.AuthenticatedUser;

@Component
public class IdentityUtility {

    public List<String> extractRoles(AuthenticatedUser user) {
        return user.roles().stream()
                .map(RoleNames::normalize)
                .filter(role -> !role.isBlank())
                .distinct()
                .toList();
    }
}
