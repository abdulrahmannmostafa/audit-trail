package com.qeema.util.security.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api")
public class RoleTestController {

    @GetMapping("/user/dashboard")
    public Map<String, String> userDashboard() {
        return Map.of("message", "You have access to the user dashboard");
    }

    @GetMapping("/admin/dashboard")
    public Map<String, String> adminDashboard() {
        return Map.of("message", "You have access to the admin dashboard");
    }
}