package com.qeema.util.audit.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.qeema.util.audit.repository.AuditLogRepository;

@Component("auditAuthorization")
public class AuditAuthorizationService {

    private final AuditLogRepository repository;

    public AuditAuthorizationService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public boolean canRead(Authentication authentication, String actorId) {
        return isAdmin(authentication) || actorId == null || actorId.equals(subject(authentication));
    }

    public boolean canReadEvent(Authentication authentication, String eventId) {
        return isAdmin(authentication) || repository.findByEventId(eventId).map(log -> log.getActorId().equals(subject(authentication))).orElse(false);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private String subject(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken().getSubject();
        }
        return authentication == null ? null : authentication.getName();
    }
}
