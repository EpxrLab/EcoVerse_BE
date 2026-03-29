package com.sep490.ecoverse_be.dto.request;

import com.sep490.ecoverse_be.enums.QuizDifficulty;
import com.sep490.ecoverse_be.enums.WasteCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class AdminGameLevelPresetUpsertRequest {

    @NotNull
    private QuizDifficulty difficulty;

    @NotEmpty
    private Set<WasteCategory> wasteCategories;

    @Valid
    @NotEmpty
    private List<AdminGameLevelPresetItemUpsertRequest> items;
}

