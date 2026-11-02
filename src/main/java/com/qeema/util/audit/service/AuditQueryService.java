package com.qeema.util.audit.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.qeema.util.audit.entity.AuditLog;
import com.qeema.util.audit.repository.AuditLogRepository;

import static com.qeema.util.audit.repository.AuditLogSpecifications.*;

import com.qeema.util.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditQueryService implements AuditQueryUseCase {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT_FIELD = "occurredAt";

    private final AuditLogRepository repository;

    // Search for audit logs based on the provided parameters and return a paginated
    // result
    @Transactional(readOnly = true)
    public Page<AuditLog> search(
            String actorId,
            String action,
            String entityType,
            String entityId,
            Instant from,
            Instant to,
            int page,
            int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Page must be non-negative and size must be between 1 and 100");
        }
        if (from != null && to != null && !from.isBefore(to)) {
            throw new IllegalArgumentException("'from' must be before 'to'");
        }
        return repository.findAll(actorIs(normalize(actorId))
                        .and(actionIs(normalize(action)))
                        .and(entityTypeIs(normalize(entityType)))
                        .and(entityIdIs(normalize(entityId)))
                        .and(occurredFrom(from))
                        .and(occurredBefore(to)),
                PageRequest.of(page, size,
                        Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD)));
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> searchFor(
            Authentication authentication,
            String actorId,
            String action,
            String entityType,
            String entityId,
            Instant from,
            Instant to,
            int page,
            int size) {
        String effectiveActor = isAdmin(authentication)
                ? actorId
                : jwtSubject(authentication);
        return search(effectiveActor, action, entityType, entityId, from, to, page, size);
    }

    // Get an audit log by its event ID
    @Transactional(readOnly = true)
    public AuditLog get(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Event ID must not be blank");
        }
        return repository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Audit event not found: " + eventId));
    }

    // Normalize a string value by trimming whitespace and returning null if it is
    // blank
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private String jwtSubject(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken().getSubject();
        }
        return authentication == null ? null : authentication.getName();
    }
}
