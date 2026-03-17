package com.sep490.ecoverse_be.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3PresignedUrlService {

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-duration-days:7}")
    private int presignedUrlDurationDays;

    /**
     * Generates a presigned GET URL for the given S3 key or full S3 URL.
     * Handles backward compatibility: if a full S3 URL is passed, the key is extracted.
     *
     * @param keyOrUrl S3 key (e.g. "ecoverse/user/avatar.png") or full URL
     * @return presigned URL, or null if input is null/blank
     */
    public String generatePresignedUrl(String keyOrUrl) {
        if (keyOrUrl == null || keyOrUrl.isBlank()) {
            return null;
        }

        String key = extractKey(keyOrUrl);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofDays(presignedUrlDurationDays))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    /**
     * Extracts the S3 key from a full S3 URL or returns the input as-is if it's already a key.
     */
    private String extractKey(String keyOrUrl) {
        // Handle full S3 URL format: https://<bucket>.s3.<region>.amazonaws.com/<key>
        if (keyOrUrl.startsWith("https://") || keyOrUrl.startsWith("http://")) {
            int amazonIndex = keyOrUrl.indexOf(".amazonaws.com/");
            if (amazonIndex != -1) {
                return keyOrUrl.substring(amazonIndex + ".amazonaws.com/".length());
            }
        }
        return keyOrUrl;
    }
}
