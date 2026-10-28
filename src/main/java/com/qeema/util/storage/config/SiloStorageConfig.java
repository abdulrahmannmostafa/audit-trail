package com.qeema.util.storage.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class SiloStorageConfig {

    @Bean
    public S3Client s3Client(
            @Value("${app.storage.silo.endpoint:http://localhost:9100}") String endpoint,
            @Value("${app.storage.silo.region:us-east-1}") String region,
            @Value("${app.storage.silo.access-key:silo-admin}") String accessKey,
            @Value("${app.storage.silo.secret-key:silo-password-change-me}") String secretKey) {

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build())
                .build();
    }
}
