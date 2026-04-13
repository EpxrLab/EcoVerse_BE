package com.sep490.ecoverse_be.dto.request;

import com.sep490.ecoverse_be.enums.WasteCategory;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MapGameTypeWasteCategoriesRequest {

    @NotEmpty
    private List<WasteCategory> wasteCategories;
}

