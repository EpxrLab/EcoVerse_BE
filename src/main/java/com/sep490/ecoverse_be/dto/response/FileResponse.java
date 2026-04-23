package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.FileCategory;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
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
        OffsetDateTime createdAt
) {
}
