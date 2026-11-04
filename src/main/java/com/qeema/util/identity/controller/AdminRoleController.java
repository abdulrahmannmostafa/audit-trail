package com.qeema.util.identity.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.qeema.util.identity.dto.RoleResponse;
import com.qeema.util.identity.dto.UserResponse;
import com.qeema.util.identity.service.UserRoleService;

@RestController
@RequestMapping("/api/admin")
public class AdminRoleController {

    private final UserRoleService userRoleService;

    public AdminRoleController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    @GetMapping("/roles")
    public List<RoleResponse> getRoles(Authentication authentication) {
        return userRoleService.getRoles(authentication);
    }

    @GetMapping("/users")
    public List<UserResponse> getUsers() {
        return userRoleService.getUsers();
    }

    @GetMapping("/users/{userId}/roles")
    public List<String> getUserRoles(@PathVariable String userId, Authentication authentication) {
        return userRoleService.getRoleNames(userId, authentication);
    }

    @PostMapping("/users/{userId}/roles/{roleName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignRole(@PathVariable String userId, @PathVariable String roleName, Authentication authentication) {
        userRoleService.assignRole(userId, roleName, authentication);
    }

    @DeleteMapping("/users/{userId}/roles/{roleName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRole(@PathVariable String userId, @PathVariable String roleName, Authentication authentication) {
        userRoleService.removeRole(userId, roleName, authentication);
    }

}