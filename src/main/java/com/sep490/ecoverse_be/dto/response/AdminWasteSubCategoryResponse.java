package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.WasteCategory;
import lombok.Builder;

import java.util.UUID;

@Builder
public record AdminWasteSubCategoryResponse(
        UUID id,
        WasteCategory category,
        String subCategoryCode,
        String displayName,
        String description,
        String iconUrl,
        String iconPresignedUrl,
        int displayOrder,
        boolean isActive
) {
}

