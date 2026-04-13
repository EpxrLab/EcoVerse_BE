package com.sep490.ecoverse_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AdminWasteItemUpsertRequest {

    @NotBlank
    private String itemName;

    @NotNull
    private UUID subCategoryId;

    private String description;

    private String funFact;

    private String imageUrl;

    private String decompositionTime;

    private String recyclingTips;

    private Boolean isActive;
}

