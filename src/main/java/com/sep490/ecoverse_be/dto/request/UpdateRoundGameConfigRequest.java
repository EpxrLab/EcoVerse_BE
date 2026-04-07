package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.Valid;
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

    @NotEmpty
    private List<UUID> selectedPresetIds;

    @Valid
    @NotEmpty
    private List<RoundPresetSubCategoryConfigRequest> presetSubCategoryConfigs;

    private Integer coinPerSession;
}
