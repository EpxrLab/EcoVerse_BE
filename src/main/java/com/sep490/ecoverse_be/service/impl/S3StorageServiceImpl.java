package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.response.StorageResponse;
import com.sep490.ecoverse_be.exception.FuncErrorException;
import com.sep490.ecoverse_be.service.IStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

import java.io.File;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class S3StorageServiceImpl implements IStorageService {

    private final S3Client s3Client;
    private final S3TransferManager s3TransferManager;
    private final S3PresignedUrlService s3PresignedUrlService;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Override
    public StorageResponse uploadFile(final MultipartFile file, final String fileName) {
        try {
            String key = "ecoverse/user/" + fileName;

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentDisposition("inline")
                    .build();

            s3Client.putObject(putRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String presignedUrl = s3PresignedUrlService.generatePresignedUrl(key);
            return StorageResponse.builder().publicId(key).url(presignedUrl).build();
        } catch (IOException e) {
            throw new FuncErrorException("Failed to upload file: " + e.getMessage());
        }
    }

    @Override
    public StorageResponse uploadModelFile(final MultipartFile file, final String fileName) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("ecoverse_model_", "_" + fileName);
            file.transferTo(tempFile);

            String key = "ecoverse/models/" + fileName;

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3TransferManager.uploadFile(
                    UploadFileRequest.builder()
                            .putObjectRequest(putRequest)
                            .source(tempFile.toPath())
                            .build()
            ).completionFuture().join();

            String presignedUrl = s3PresignedUrlService.generatePresignedUrl(key);
            return StorageResponse.builder().publicId(key).url(presignedUrl).build();
        } catch (IOException e) {
            throw new FuncErrorException("Failed to process 3D model file: " + e.getMessage());
        } catch (Exception e) {
            throw new FuncErrorException("Failed to upload 3D model: " + e.getMessage());
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
