package com.qeema.util.common.security;

import java.util.Locale;

public final class RoleNames {

    private RoleNames() {
    }

    public static String normalize(String role) {
        return role == null ? null : role.trim().toUpperCase(Locale.ROOT);
    }
}
