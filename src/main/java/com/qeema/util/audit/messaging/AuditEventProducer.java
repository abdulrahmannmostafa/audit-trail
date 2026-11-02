package com.qeema.util.audit.messaging;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.qeema.util.common.exception.AuditPublishingException;

@Component
public class AuditEventProducer implements AuditEventPublisher {

    public static final String TOPIC = "audit-events";
    private static final int TIMEOUT_SECONDS = 10;
    private static final Logger logger = LoggerFactory.getLogger(AuditEventProducer.class);

    private final KafkaTemplate<String, AuditEvent> kafkaTemplate;

    public AuditEventProducer(KafkaTemplate<String, AuditEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(AuditEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Audit event must not be null");
        }
        try {

            // Try to send the audit event to the Kafka topic and wait for the result with a
            // timeout of 10 seconds
            logger.debug("Publishing audit event {} to topic {}", event.getEventId(), TOPIC);
            kafkaTemplate.send(TOPIC, event.getActorId(), event)
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        } catch (InterruptedException exception) {
            // Restore the interrupted status of the thread and throw a custom exception
            // If i don't interrupted, the current thread may not respond to future
            // interrupts which can lead to unexpected behavior
            logger.error("Audit event publishing was interrupted for event {}", event.getEventId(), exception);
            Thread.currentThread().interrupt();

            throw new AuditPublishingException(
                    "Audit event publishing was interrupted", exception);
        } catch (TimeoutException | ExecutionException exception) {
            logger.error("Failed to publish audit event {}", event.getEventId(), exception);

            throw new AuditPublishingException(
                    "Audit event could not be published", exception);
        }
    }
}
