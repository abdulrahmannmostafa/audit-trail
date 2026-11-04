package com.qeema.util.audit.aspect;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.qeema.util.audit.annotation.Audited;
import com.qeema.util.audit.service.AuditService;

import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuditAspect.class);
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILURE = "FAILURE";
    private static final String METHOD = "method";
    private static final String DURATION_MS = "durationMs";
    private static final String ERROR_TYPE = "errorType";

    private final AuditService auditService;

    @Around("@annotation(audited)")
    public Object recordAudit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        Instant startedAt = Instant.now();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        try {
            Object result = joinPoint.proceed();
            record(joinPoint, authentication, audited, SUCCESS, startedAt, null);
            return result;
        } catch (Throwable exception) {
            record(joinPoint, authentication, audited, FAILURE, startedAt, exception);
            throw exception;
        }
    }

    private void record(
            ProceedingJoinPoint joinPoint,
            Authentication authentication,
            Audited audited,
            String outcome,
            Instant startedAt,
            Throwable exception) {
        try {
            Map<String, Object> metadata = exception == null
                    ? Map.of(METHOD, methodName(joinPoint), DURATION_MS, elapsedMillis(startedAt))
                    : Map.of(
                            METHOD, methodName(joinPoint),
                            DURATION_MS, elapsedMillis(startedAt),
                            ERROR_TYPE, exception.getClass().getSimpleName());

            auditService.recordAuditEvent(
                    authentication,
                    audited.action(),
                    audited.entityType(),
                    methodName(joinPoint),
                    outcome,
                    metadata);
        } catch (RuntimeException auditException) {
            logger.warn("Unable to record aspect audit event for {}", audited.action(), auditException);
        }
    }

    private String methodName(ProceedingJoinPoint joinPoint) {
        return joinPoint.getSignature().toShortString();
    }

    private long elapsedMillis(Instant startedAt) {
        return Duration.between(startedAt, Instant.now()).toMillis();
    }
}
