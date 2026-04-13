package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.FileCategory;

import java.time.LocalDateTime;
import java.util.UUID;

public record FileResponse(
        UUID id,
        String fileName,
        String fileUrl,
        String filePresignedUrl,
        String fileType,
        Long fileSize,
        String uploadedByEmail,
        FileCategory category,
        LocalDateTime createdAt
) {
}
