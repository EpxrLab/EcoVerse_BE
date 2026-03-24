package com.sep490.ecoverse_be.dto.request;

import com.sep490.ecoverse_be.enums.WasteCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminWasteSubCategoryUpsertRequest {

    @NotNull
    private WasteCategory category;

    @NotBlank
    private String subCategoryCode;

    @NotBlank
    private String displayName;

    private String description;

    private String iconUrl;

    private Integer displayOrder;

    private Boolean isActive;
}

