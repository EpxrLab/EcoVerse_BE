package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.GameTypeCode;
import com.sep490.ecoverse_be.enums.WasteCategory;
import lombok.Builder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
public record AdminGameTypeResponse(
        UUID id,
        GameTypeCode typeCode,
        String name,
        String shortDescription,
        String fullDescription,
        String howToPlay,
        String thumbnailUrl,
        String iconUrl,
        String previewVideoUrl,
        Map<String, Object> features,
        boolean supportsCoin,
        int maxLevels,
        boolean isActive,
        int displayOrder,
        List<WasteCategory> mappedWasteCategories,
        List<UUID> supportedSubCategoryIds
) {
}

