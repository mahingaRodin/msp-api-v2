package com.msp.services;

import com.msp.payloads.response.PresignedUrlResult;
import java.io.InputStream;
import java.time.Duration;

public interface AwsS3Service {
    String uploadDocument(String tenantId, String fileName, InputStream content, String contentType);
    PresignedUrlResult generatePresignedDownloadUrl(String s3Key, Duration expiry);
    void deleteDocument(String s3Key);
}
