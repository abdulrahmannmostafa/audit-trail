package com.qeema.util.storage.utility;

import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StorageUtility {

    private final S3Client s3Client;

    public void ensureBucket(String bucket) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (S3Exception exception) {
            if (exception.statusCode() != 404) {
                throw exception;
            }
            s3Client.createBucket(builder -> builder.bucket(bucket));
        }
    }
}
