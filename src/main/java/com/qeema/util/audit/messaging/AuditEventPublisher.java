package com.qeema.util.audit.messaging;

public interface AuditEventPublisher {
    void publish(AuditEvent event);
}
