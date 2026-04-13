package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class RoundPresetSubCategoryConfigRequest {

    @NotNull
    private UUID presetId;

    @NotEmpty
    private List<UUID> selectedSubCategoryIds;
}

