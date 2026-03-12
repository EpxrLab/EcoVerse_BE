package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.exception.FuncErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class S3FileService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    public Map<String, Object> upload(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String key = "ecoverse/files/" + originalFilename;

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);

            Map<String, Object> result = new HashMap<>();
            result.put("secure_url", url);
            result.put("public_id", key);
            return result;
        } catch (IOException e) {
            throw new FuncErrorException("Failed to upload file to S3: " + e.getMessage());
        }
    }

    public void delete(String key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
        } catch (Exception e) {
            throw new FuncErrorException("Failed to delete file from S3: " + e.getMessage());
        }
    }
}
