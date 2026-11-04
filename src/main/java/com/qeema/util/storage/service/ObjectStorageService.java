package com.qeema.util.storage.service;

import java.io.InputStream;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@RequiredArgsConstructor
public class ObjectStorageService implements ObjectStorageGateway {

    private final S3Client s3Client;

    @Value("${app.storage.silo.bucket}")
    private String bucket;

    public void upload(String key, InputStream inputStream, long contentLength, String contentType) {

        ensureBucket();
        PutObjectRequest request = PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build();

        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
    }

    private void ensureBucket() {
        final int ERROR_CODE = 404;

        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (S3Exception exception) {
            if (exception.statusCode() != ERROR_CODE) {
                throw exception;
            }
            s3Client.createBucket(builder -> builder.bucket(bucket));
        }
    }

    public ResponseInputStream<GetObjectResponse> download(String key) {
        GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(key).build();

        return s3Client.getObject(request);
    }

    public void delete(String key) {
        s3Client.deleteObject(builder -> builder.bucket(bucket).key(key));
    }
}
