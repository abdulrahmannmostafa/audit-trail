package com.qeema.util.audit.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import com.qeema.util.audit.repository.AuditLogRepository;
import com.qeema.util.common.security.AuthenticationIdentityUtility;

@Component("auditAuthorization")
@RequiredArgsConstructor
public class AuditAuthorizationService {

    private final AuditLogRepository repository;

    public boolean canRead(Authentication authentication, String actorId) {
        return AuthenticationIdentityUtility.isAdmin(authentication) || actorId == null
                || actorId.equals(AuthenticationIdentityUtility.subject(authentication));
    }

    public boolean canReadEvent(Authentication authentication, String eventId) {
        return AuthenticationIdentityUtility.isAdmin(authentication) || repository.findByEventId(eventId)
                .map(log -> log.getActorId().equals(AuthenticationIdentityUtility.subject(authentication)))
                .orElse(false);
    }

}
