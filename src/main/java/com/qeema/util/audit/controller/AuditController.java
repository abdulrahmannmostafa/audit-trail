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
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;

import com.qeema.util.audit.dto.AuditLogResponse;
import com.qeema.util.audit.entity.AuditLog;
import com.qeema.util.audit.annotation.Audited;
import com.qeema.util.audit.constants.AuditConstants;
import com.qeema.util.audit.mapper.AuditLogMapper;
import com.qeema.util.audit.service.AuditQueryUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/audit-logs")
@Tag(name = "Audit logs", description = "Read-only audit history")
@RequiredArgsConstructor
public class AuditController {

        private final AuditQueryUseCase queryService;
        private final AuditLogMapper mapper;

        @GetMapping
        @Operation(summary = "Search audit logs")
        @Audited(action = AuditConstants.SEARCHED, entityType = AuditConstants.ENTITY_TYPE)
        @PreAuthorize("isAuthenticated()")
        public Page<AuditLogResponse> search(
                        @RequestParam(required = false) String actorId,
                        @RequestParam(required = false) String action,
                        @RequestParam(required = false) String entityType,
                        @RequestParam(required = false) String entityId,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "50") int size,
                        Authentication authentication) {
                ZoneId zone = ZoneId.systemDefault();
                Instant fromInstant = from == null ? null
                                : from.atStartOfDay(zone).toInstant();

                Instant toInstant = to == null ? null
                                : to.plusDays(1).atStartOfDay(zone).toInstant();

                return queryService.searchForAuditLogs(authentication, actorId, action, entityType, entityId,
                                fromInstant, toInstant, page, size)
                                .map(mapper::toResponse);
        }

        @GetMapping("/{eventId}")
        @Operation(summary = "Get an audit event")
        @Audited(action = AuditConstants.VIEWED, entityType = AuditConstants.ENTITY_TYPE)
        @PreAuthorize("hasRole('ADMIN') or @auditAuthorization.canReadEvent(authentication, #eventId)")
        public ResponseEntity<AuditLogResponse> get(
                        @PathVariable String eventId) {
                return ResponseEntity.ok(mapper.toResponse(queryService.get(eventId)));
        }

}
