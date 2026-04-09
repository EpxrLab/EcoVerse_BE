package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.WasteCategory;
import lombok.Builder;

import java.util.UUID;

@Builder
public record GameLevelWasteItemResponse(
        UUID wasteItemId,
        String itemName,
        WasteCategory wasteCategory,
        UUID subCategoryId,
        String subCategoryCode,
        String subCategoryDisplayName,
        String imageUrl,
        String imagePresignedUrl
) {
}
