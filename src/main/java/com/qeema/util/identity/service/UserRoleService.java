
package com.qeema.util.identity.service;

import com.qeema.util.identity.entity.AppRole;
import com.qeema.util.identity.entity.User;
import com.qeema.util.identity.entity.UserRole;
import com.qeema.util.identity.repository.AppRoleRepository;
import com.qeema.util.identity.repository.UserRepository;
import com.qeema.util.identity.repository.UserRoleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final AppRoleRepository appRoleRepository;

    public UserRoleService(
            UserRoleRepository userRoleRepository,
            UserRepository userRepository,
            AppRoleRepository appRoleRepository) {
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
        this.appRoleRepository = appRoleRepository;
    }

    @Transactional(readOnly = true)
    public List<String> getRoleNames(String userId) {
        return userRoleRepository.findByUserId(userId)
                .stream()
                .map(userRole -> userRole.getRole().getName())
                .map(name -> name.toUpperCase(Locale.ROOT))
                .distinct()
                .sorted()
                .toList();
    }

    @Transactional
    public void assignRole(String userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String normalizedName = roleName.trim().toUpperCase(Locale.ROOT);

        AppRole role = appRoleRepository.findByName(normalizedName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        if (!userRoleRepository.existsByUserIdAndRoleId(
                userId, role.getId().toString())) {
            userRoleRepository.save(new UserRole(user, role));
        }
    }

    @Transactional
    public void removeRole(String userId, String roleName) {
        String normalizedName = roleName.trim().toUpperCase(Locale.ROOT);
        AppRole role = appRoleRepository.findByName(normalizedName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        userRoleRepository.deleteByUserIdAndRoleId(
                userId, role.getId().toString());
    }
}
