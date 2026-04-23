package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.RewardType;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RewardResponse {

    private UUID id;
    private String rewardName;
    private RewardType rewardType;
    private String description;
    private BigDecimal coinCost;
    private String imageUrl;
    private String imagePresignedUrl;
    private Integer stockQuantity;
    private Boolean isUnlimited;
    private Boolean isActive;
    private String termsConditions;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
