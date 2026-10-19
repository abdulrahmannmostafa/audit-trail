package com.qeema.util.identity.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.qeema.util.identity.repository.AppRoleRepository;
import com.qeema.util.identity.service.UserRoleService;

@RestController
@RequestMapping("/api/admin")
public class AdminRoleController {

    private final AppRoleRepository roleRepository;
    private final UserRoleService userRoleService;

    public AdminRoleController(
            AppRoleRepository roleRepository,
            UserRoleService userRoleService) {
        this.roleRepository = roleRepository;
        this.userRoleService = userRoleService;
    }

    @GetMapping("/roles")
    public List<RoleResponse> getRoles() {
        return roleRepository.findAll().stream()
                .map(role -> new RoleResponse(
                        role.getId(),
                        role.getName()))
                .toList();
    }

    @GetMapping("/users/{userId}/roles")
    public List<String> getUserRoles(
            @PathVariable String userId) {
        return userRoleService.getRoleNames(userId);
    }

    @PostMapping("/users/{userId}/roles/{roleName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignRole(
            @PathVariable String userId,
            @PathVariable String roleName) {
        userRoleService.assignRole(userId, roleName);
    }

    @DeleteMapping("/users/{userId}/roles/{roleName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRole(
            @PathVariable String userId,
            @PathVariable String roleName) {
        userRoleService.removeRole(userId, roleName);
    }

    public record RoleResponse(
            Integer id,
            String name) {
    }
}