package com.qeema.util;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	KafkaContainer kafkaContainer() {
		return new KafkaContainer(DockerImageName.parse("apache/kafka-native:latest"));
	}

	@Bean
	@ServiceConnection
	MySQLContainer mysqlContainer() {
		return new MySQLContainer(DockerImageName.parse("mysql:latest"));
	}

	@Bean
	JwtDecoder testJwtDecoder() {
		return token -> Jwt.withTokenValue(token)
				.header("alg", "none")
				.claim("sub", "test-user")
				.issuer("http://localhost:8180/realms/demo")
				.build();
	}

}
