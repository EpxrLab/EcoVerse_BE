package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.WasteCategory;
import lombok.Builder;

import java.util.UUID;

@Builder
public record AdminWasteItemResponse(
        UUID id,
        String itemName,
        WasteCategory category,
        UUID subCategoryId,
        String subCategoryCode,
        String subCategoryDisplayName,
        String description,
        String funFact,
        String imageUrl,
        String imagePresignedUrl,
        String decompositionTime,
        String recyclingTips,
        String model3dUrl,
        String model3dPresignedUrl,
        String tripoTaskId,
        String tripoStatus,
        boolean isActive
) {
}

