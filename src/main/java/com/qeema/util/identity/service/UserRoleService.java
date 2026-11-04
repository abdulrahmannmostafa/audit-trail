package com.qeema.util.identity.service;

import com.qeema.util.identity.entity.AppRole;
import com.qeema.util.identity.entity.User;
import com.qeema.util.identity.entity.UserRole;
import com.qeema.util.identity.dto.RoleResponse;
import com.qeema.util.identity.dto.UserResponse;
import com.qeema.util.common.exception.ResourceNotFoundException;
import com.qeema.util.identity.repository.AppRoleRepository;
import com.qeema.util.identity.repository.UserRepository;
import com.qeema.util.identity.repository.UserRoleRepository;
import com.qeema.util.common.security.RoleNames;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import com.qeema.util.audit.service.AuditService;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import com.qeema.util.identity.constants.IdentityConstants;
import com.qeema.util.identity.mapper.RoleMapper;
import com.qeema.util.identity.mapper.UserMapper;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final AppRoleRepository appRoleRepository;
    private final AuditService auditService;
    private final RoleMapper roleMapper;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getRoleNames(String userId) {
        return getRoleNames(userId, null);
    }

    @Transactional(readOnly = true)
    public List<String> getRoleNames(String userId, Authentication authentication) {
        List<String> roles = userRoleRepository.findByUserId(userId).stream()
                .map(userRole -> userRole.getRole().getName()).map(RoleNames::normalize).distinct().sorted().toList();

        auditService.record(authentication, IdentityConstants.USER_ROLES_VIEWED, IdentityConstants.USER_ENTITY,
                userId, IdentityConstants.SUCCESS,
                Map.of("count", roles.size()));

        return roles;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles() {
        return getRoles(null);
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles(Authentication authentication) {
        List<RoleResponse> roles = appRoleRepository.findAll().stream()
                .map(roleMapper::toResponse).toList();

        auditService.record(authentication, IdentityConstants.ROLES_LISTED, IdentityConstants.ROLE_ENTITY,
                IdentityConstants.COLLECTION, IdentityConstants.SUCCESS,
                Map.of("count", roles.size()));

        return roles;
    }

    @Transactional
    public void assignRole(String userId, String roleName) {
        assignRole(userId, roleName, null);
    }

    @Transactional
    public void assignRole(String userId, String roleName, Authentication authentication) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String normalizedName = RoleNames.normalize(roleName);

        AppRole role = appRoleRepository.findByName(normalizedName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (!userRoleRepository.existsByUserIdAndRoleId(userId, role.getId().toString())) {

            userRoleRepository.save(new UserRole(user, role));

            auditService.record(authentication, IdentityConstants.ROLE_ASSIGNED, IdentityConstants.USER_ENTITY,
                    userId, IdentityConstants.SUCCESS,
                    Map.of(), Map.of("role", normalizedName), Map.of("role", normalizedName));
        }

    }

    @Transactional
    public void removeRole(String userId, String roleName) {
        removeRole(userId, roleName, null);
    }

    @Transactional
    public void removeRole(String userId, String roleName, Authentication authentication) {
        String normalizedName = RoleNames.normalize(roleName);

        AppRole role = appRoleRepository.findByName(normalizedName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        userRoleRepository.deleteByUserIdAndRoleId(userId, role.getId().toString());

        auditService.record(authentication, IdentityConstants.ROLE_REMOVED, IdentityConstants.USER_ENTITY,
                userId, IdentityConstants.SUCCESS,
                Map.of("role", normalizedName), Map.of(), Map.of("role", normalizedName));
    }
}
