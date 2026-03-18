package com.sep490.ecoverse_be.mapper;

import com.sep490.ecoverse_be.dto.response.FileResponse;
import com.sep490.ecoverse_be.entity.FileEntity;
import com.sep490.ecoverse_be.service.impl.S3PresignedUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileMapper {

    private final S3PresignedUrlService s3PresignedUrlService;

    public FileResponse toResponse(FileEntity file) {
        return new FileResponse(
                file.getId(),
                file.getFileName(),
                s3PresignedUrlService.generatePresignedUrl(file.getFileUrl()),
                file.getFileType(),
                file.getFileSize(),
                file.getUploadedBy() != null ? file.getUploadedBy().getEmail() : null,
                file.getCategory(),
                file.getCreatedAt()
        );
    }
}
