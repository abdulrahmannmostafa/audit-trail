package com.qeema.util.identity.service;

import com.qeema.util.identity.entity.AppRole;
import com.qeema.util.identity.entity.User;
import com.qeema.util.identity.entity.UserRole;
import com.qeema.util.identity.dto.RoleResponse;
import com.qeema.util.common.exception.ResourceNotFoundException;
import com.qeema.util.identity.repository.AppRoleRepository;
import com.qeema.util.identity.repository.UserRepository;
import com.qeema.util.identity.repository.UserRoleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import com.qeema.util.audit.service.AuditService;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final String USER_ENTITY_TYPE = "USER";
    private final String ROLE_ENTITY_TYPE = "ROLE";

    private final String USER_SUCCESS_ACTION = "SUCCESS";

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final AppRoleRepository appRoleRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<String> getRoleNames(String userId) {
        return getRoleNames(userId, null);
    }

    @Transactional(readOnly = true)
    public List<String> getRoleNames(String userId, Authentication authentication) {
        final String USER_ROLES_VIEWED_ACTION = "USER_ROLES_VIEWED";

        List<String> roles = userRoleRepository.findByUserId(userId).stream().map(userRole -> userRole.getRole().getName()).map(name -> name.toUpperCase(Locale.ROOT)).distinct().sorted().toList();
        auditService.record(authentication, USER_ROLES_VIEWED_ACTION, USER_ENTITY_TYPE, userId, USER_SUCCESS_ACTION, Map.of("count", roles.size()));
        return roles;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles() {
        return getRoles(null);
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles(Authentication authentication) {
        final String ROLES_LISTED_ACTION = "ROLES_LISTED";
        final String ROLE_ENTITY_ID = "collection";

        List<RoleResponse> roles = appRoleRepository.findAll().stream().map(role -> new RoleResponse(role.getId(), role.getName())).toList();
        auditService.record(authentication, ROLES_LISTED_ACTION, ROLE_ENTITY_TYPE, ROLE_ENTITY_ID, USER_SUCCESS_ACTION, Map.of("count", roles.size()));
        return roles;
    }

    @Transactional
    public void assignRole(String userId, String roleName) {
        assignRole(userId, roleName, null);
    }

    @Transactional
    public void assignRole(String userId, String roleName, Authentication authentication) {
        final String ROLE_ASSIGNED_ACTION = "ROLE_ASSIGNED";

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String normalizedName = roleName.trim().toUpperCase(Locale.ROOT);

        AppRole role = appRoleRepository.findByName(normalizedName).orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (!userRoleRepository.existsByUserIdAndRoleId(userId, role.getId().toString())) {
            userRoleRepository.save(new UserRole(user, role));
            auditService.record(authentication, ROLE_ASSIGNED_ACTION, USER_ENTITY_TYPE, userId, USER_SUCCESS_ACTION, Map.of(), Map.of("role", normalizedName), Map.of("role", normalizedName));
        }

    }

    @Transactional
    public void removeRole(String userId, String roleName) {
        removeRole(userId, roleName, null);
    }

    @Transactional
    public void removeRole(String userId, String roleName, Authentication authentication) {
        final String ROLE_REMOVED_ACTION = "ROLE_REMOVED";

        String normalizedName = roleName.trim().toUpperCase(Locale.ROOT);
        AppRole role = appRoleRepository.findByName(normalizedName).orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        userRoleRepository.deleteByUserIdAndRoleId(userId, role.getId().toString());
        auditService.record(authentication, ROLE_REMOVED_ACTION, USER_ENTITY_TYPE, userId, USER_SUCCESS_ACTION, Map.of("role", normalizedName), Map.of(), Map.of("role", normalizedName));
    }
}
