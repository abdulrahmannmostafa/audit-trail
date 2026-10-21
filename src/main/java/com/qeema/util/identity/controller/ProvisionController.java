package com.qeema.util.identity.controller;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qeema.util.identity.dto.UserResponse;
import com.qeema.util.identity.service.UserProvisioningService;

@RestController
@RequestMapping("/api")
public class ProvisionController {

    private final UserProvisioningService provisioningService;

    public ProvisionController(
            UserProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    @GetMapping("/public")
    public Map<String, String> publicEndpoint() {
        return Map.of("message", "This endpoint is public");
    }

    @GetMapping("/me")
    public UserResponse currentUser(
            @AuthenticationPrincipal Jwt jwt) {
        return provisioningService.provision(jwt);
    }
}