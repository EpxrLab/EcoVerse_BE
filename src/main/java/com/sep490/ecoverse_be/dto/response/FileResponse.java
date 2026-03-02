package com.sep490.ecoverse_be.dto.response;

import java.time.LocalDateTime;

public record FileResponse(
        Long id,
        String fileName,
        String fileUrl,
        String fileType,
        Long fileSize,
        String uploadedByEmail,
        LocalDateTime createdAt
) {
}
