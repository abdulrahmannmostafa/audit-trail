package com.qeema.util.identity.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.qeema.util.identity.dto.RoleResponse;
import com.qeema.util.identity.service.UserRoleService;

@RestController
@RequestMapping("/api/admin")
public class AdminRoleController {

    private final UserRoleService userRoleService;

    public AdminRoleController(
            UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    @GetMapping("/roles")
    public List<RoleResponse> getRoles() {
        return userRoleService.getRoles();
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

}