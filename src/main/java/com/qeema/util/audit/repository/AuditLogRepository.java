package com.qeema.util.audit.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

import com.qeema.util.audit.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, String>,
    JpaSpecificationExecutor<AuditLog> {

  boolean existsByEventId(String eventId);

  Optional<AuditLog> findByEventId(String eventId);

  // Make a join query to search for audit logs based on the provided parameters
  // and return a paginated result
  @Query("""
      select a from AuditLog a
      where (:actorId is null or a.actorId = :actorId)
        and (:action is null or a.action = :action)
        and (:entityType is null or a.entityType = :entityType)
        and (:entityId is null or a.entityId = :entityId)
        and (:from is null or a.occurredAt >= :from)
        and (:to is null or a.occurredAt < :to)
      order by a.occurredAt desc
      """)
  Page<AuditLog> search(
      @Param("actorId") String actorId,
      @Param("action") String action,
      @Param("entityType") String entityType,
      @Param("entityId") String entityId,
      @Param("from") Instant from,
      @Param("to") Instant to,
      Pageable pageable);
}
