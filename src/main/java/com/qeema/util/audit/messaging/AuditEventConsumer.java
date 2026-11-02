package com.qeema.util.audit.messaging;

import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.qeema.util.audit.entity.AuditLog;
import com.qeema.util.audit.repository.AuditLogRepository;

@Component
public class AuditEventConsumer {

    private final AuditLogRepository repository;
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AuditEventConsumer.class);

    public AuditEventConsumer(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @KafkaListener(topics = AuditEventProducer.TOPIC)
    public void consume(AuditEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Received null audit event");
        }

        String eventId = event.getEventId().toString();
        logger.debug("Consuming audit event {}", eventId);

        // Check if the event has already been consumed to avoid duplicates
        if (repository.existsByEventId(eventId)) {
            logger.debug("Audit event {} has already been consumed", eventId);
            return;
        }

        try {
            logger.debug("Saving audit event {} to the database", eventId);
            repository.saveAndFlush(AuditLog.from(event));
        } catch (DataIntegrityViolationException exception) {
            /*
             * Two consumer instances can pass the pre-check concurrently
             * The unique (event_id) index is the atomic idempotency barrier
             * Rethrow any other integrity failure so Kafka retries it
             */
            if (repository.existsByEventId(eventId)) {
                logger.debug(
                        "Audit event {} was concurrently persisted; treating delivery as duplicate",
                        eventId);
                return;
            }
            logger.error("Failed to persist audit event {}", eventId, exception);
            if (!repository.existsByEventId(eventId)) {
                throw exception;
            }
        }
    }
}
