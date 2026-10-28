package com.qeema.util.storage.service;

import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class ObjectStorageService {

    private final S3Client s3Client;
    private final String bucket;

    public ObjectStorageService(
            S3Client s3Client,
            @Value("${app.storage.silo.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    public void upload(
            String key,
            InputStream inputStream,
            long contentLength,
            String contentType) {

        ensureBucket();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(
                request,
                RequestBody.fromInputStream(inputStream, contentLength));
    }

    private void ensureBucket() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder()
                    .bucket(bucket)
                    .build());
        } catch (software.amazon.awssdk.services.s3.model.S3Exception exception) {
            if (exception.statusCode() != 404) {
                throw exception;
            }
            s3Client.createBucket(builder -> builder.bucket(bucket));
        }
    }

    public ResponseInputStream<GetObjectResponse> download(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        return s3Client.getObject(request);
    }

    public void delete(String key) {
        s3Client.deleteObject(builder -> builder
                .bucket(bucket)
                .key(key));
    }
}
