package com.sep490.ecoverse_be.dto.request;

import com.sep490.ecoverse_be.enums.QuizDifficulty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AdminGameLevelPresetUpsertRequest {

    @NotNull
    private QuizDifficulty difficulty;

    @Valid
    @NotEmpty
    private List<AdminGameLevelPresetItemUpsertRequest> items;
}


