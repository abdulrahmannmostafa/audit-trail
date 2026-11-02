package com.qeema.util.audit.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qeema.util.audit.entity.AuditLog;
import com.qeema.util.audit.repository.AuditLogRepository;
import com.qeema.util.common.exception.ResourceNotFoundException;

@Service
public class AuditQueryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT_FIELD = "occurredAt";

    private final AuditLogRepository repository;

    public AuditQueryService(AuditLogRepository repository) {
        this.repository = repository;
    }

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
        return repository.search(
                normalize(actorId),
                normalize(action),
                normalize(entityType),
                normalize(entityId),
                from,
                to,
                PageRequest.of(page, size,
                        Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD)));
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
}
