package com.sep490.ecoverse_be.dto.request;

import com.sep490.ecoverse_be.enums.GameTypeCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class AdminGameTypeUpsertRequest {

    @NotNull
    private GameTypeCode typeCode;

    @NotBlank
    private String name;

    @NotBlank
    private String shortDescription;

    private String fullDescription;

    @NotBlank
    private String howToPlay;

    private String thumbnailUrl;

    private String iconUrl;


    private Map<String, Object> features;

    private Boolean supportsCoin;

    private Integer maxLevels;

    private Integer displayOrder;
}

