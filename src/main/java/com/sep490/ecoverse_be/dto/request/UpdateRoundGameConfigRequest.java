package com.sep490.ecoverse_be.dto.request;

import com.sep490.ecoverse_be.enums.QuizDifficulty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class UpdateRoundGameConfigRequest {

    @NotNull
    private UUID gameTypeId;

    private QuizDifficulty difficultyOverride;

    @NotEmpty
    private List<UUID> selectedSubCategoryIds;

    private Integer coinPerSession;
}

