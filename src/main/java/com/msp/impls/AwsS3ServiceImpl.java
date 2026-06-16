package com.msp.impls;

import com.msp.payloads.response.PresignedUrlResult;
import com.msp.services.AwsS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AwsS3ServiceImpl implements AwsS3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Override
    public String uploadDocument(String tenantId, String fileName, InputStream content, String contentType) {
        String s3Key = String.format("tenants/%s/docs/%s", tenantId, fileName);
        try {
            byte[] bytes = content.readAllBytes();
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(bytes));
            log.info("Document uploaded to S3: bucket={}, key={}", bucketName, s3Key);
            return s3Key;
        } catch (Exception e) {
            log.error("Failed to upload file to S3 at key {}: {}", s3Key, e.getMessage());
            throw new RuntimeException("S3 Upload Failed", e);
        }
    }

    @Override
    public PresignedUrlResult generatePresignedDownloadUrl(String s3Key, Duration expiry) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiry)
                    .getObjectRequest(getRequest)
                    .build();

            String url = s3Presigner.presignGetObject(presignRequest).url().toString();
            long expiresAt = Instant.now().plus(expiry).toEpochMilli();
            return new PresignedUrlResult(url, expiresAt);
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for key {}: {}", s3Key, e.getMessage());
            throw new RuntimeException("S3 Presign Failed", e);
        }
    }

    @Override
    public void deleteDocument(String s3Key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("S3 Document deleted: key={}", s3Key);
        } catch (Exception e) {
            log.error("Failed to delete S3 object {}: {}", s3Key, e.getMessage());
            throw new RuntimeException("S3 Deletion Failed", e);
        }
    }
}
