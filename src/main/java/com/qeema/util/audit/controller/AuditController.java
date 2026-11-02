package com.qeema.util.audit.controller;

import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qeema.util.audit.dto.AuditLogResponse;
import com.qeema.util.audit.entity.AuditLog;
import com.qeema.util.audit.service.AuditQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/audit-logs")
@Tag(name = "Audit logs", description = "Read-only audit history")
public class AuditController {

    private final AuditQueryService queryService;

    public AuditController(AuditQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "Search audit logs")
    public Page<AuditLogResponse> search(
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        ZoneId zone = ZoneId.systemDefault();
        Instant fromInstant = from == null ? null
                : from.atStartOfDay(zone).toInstant();

        Instant toInstant = to == null ? null
                : to.plusDays(1).atStartOfDay(zone).toInstant();
        return queryService.search(
                actorId, action, entityType, entityId, fromInstant, toInstant, page, size)
                .map(this::toResponse);
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Get an audit event")
    public ResponseEntity<AuditLogResponse> get(
            @PathVariable String eventId) {
        return ResponseEntity.ok(toResponse(queryService.get(eventId)));
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getEventId(),
                log.getOccurredAt(),
                log.getActorId(),
                log.getActorUsername(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getOutcome(),
                log.getOldValues(),
                log.getNewValues(),
                log.getMetadata(),
                log.getConsumedAt());
    }

}
