package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.QuizDifficulty;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record PresetAvailableSubCategoriesResponse(
        UUID presetId,
        UUID gameTypeId,
        QuizDifficulty difficulty,
        List<WasteSubCategoryOptionResponse> availableSubCategories
) {
}

