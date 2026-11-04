package com.qeema.util.storage.service;

import java.io.InputStream;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public interface ObjectStorageGateway {
    void upload(String key, InputStream inputStream, long contentLength, String contentType);

    ResponseInputStream<GetObjectResponse> download(String key);

    void delete(String key);
}
