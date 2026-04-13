package com.sep490.ecoverse_be.dto.request;

import com.sep490.ecoverse_be.enums.WasteCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Set;

@Getter
@Setter
public class AdminGameLevelPresetItemUpsertRequest {

    @NotNull
    @Min(1)
    private Integer levelNumber;

    @NotNull
    @Min(1)
    private Integer itemCount;

    @NotNull
    @Min(0)
    private Integer timeLimitSeconds;

    @NotNull
    @Min(1)
    private Integer scorePerCorrect;

    private Integer lives;

    @NotEmpty
    private Set<WasteCategory> wasteCategories;

    private Map<String, Object> configJson;
}


