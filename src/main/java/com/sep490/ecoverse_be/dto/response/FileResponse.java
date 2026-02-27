package com.sep490.ecoverse_be.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record FileResponse(
        UUID id,
        String fileName,
        String fileUrl,
        String fileType,
        Long fileSize,
        String uploadedByEmail,
        LocalDateTime createdAt
) {
}
