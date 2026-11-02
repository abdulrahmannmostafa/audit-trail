package com.qeema.util.audit.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import com.qeema.util.audit.messaging.AuditEvent;

@Configuration
public class AuditKafkaConfig {

        @Bean
        ProducerFactory<String, AuditEvent> auditProducerFactory(
                        KafkaProperties properties) {
                Map<String, Object> config = new HashMap<>(
                                properties.buildProducerProperties());
                // Set the key and value serializer classes for the producer
                config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                                StringSerializer.class);

                // Set the value serializer class for the producer to use JacksonJsonSerializer
                config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                                JacksonJsonSerializer.class);
                return new DefaultKafkaProducerFactory<>(config);
        }

        @Bean
        KafkaTemplate<String, AuditEvent> auditKafkaTemplate(
                        ProducerFactory<String, AuditEvent> producerFactory) {
                return new KafkaTemplate<>(producerFactory);
        }

        @Bean
        ConsumerFactory<String, AuditEvent> auditConsumerFactory(
                        KafkaProperties properties) {
                Map<String, Object> config = new HashMap<>(
                                properties.buildConsumerProperties());
                // Set the key and value deserializer classes for the consumer
                config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                                StringDeserializer.class);

                // Set the value deserializer class for the consumer to use
                // JacksonJsonDeserializer
                config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                                JacksonJsonDeserializer.class);

                // Set the default type for deserialization to AuditEvent and trust the package
                config.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE,
                                AuditEvent.class.getName());

                // Set the trusted packages for deserialization to the package of AuditEvent
                config.put(JacksonJsonDeserializer.TRUSTED_PACKAGES,
                                AuditEvent.class.getPackageName());

                // Disable the use of type information headers for deserialization
                config.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);
                return new DefaultKafkaConsumerFactory<>(config);
        }

        @Bean(name = "kafkaListenerContainerFactory")
        ConcurrentKafkaListenerContainerFactory<String, AuditEvent> auditKafkaListenerContainerFactory(
                        ConsumerFactory<String, AuditEvent> consumerFactory,
                        KafkaTemplate<String, AuditEvent> kafkaTemplate) {

                // Create a ConcurrentKafkaListenerContainerFactory for AuditEvent messages
                ConcurrentKafkaListenerContainerFactory<String, AuditEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
                factory.setConsumerFactory(consumerFactory);
                factory.getContainerProperties().setAckMode(
                                ContainerProperties.AckMode.RECORD);
                factory.setCommonErrorHandler(new DefaultErrorHandler(
                                new DeadLetterPublishingRecoverer(kafkaTemplate),
                                new FixedBackOff(1_000L, 3L)));
                return factory;
        }
}
