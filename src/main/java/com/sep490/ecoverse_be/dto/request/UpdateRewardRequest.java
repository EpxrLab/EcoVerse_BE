package com.sep490.ecoverse_be.dto.request;

import com.sep490.ecoverse_be.enums.RewardType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateRewardRequest {

    @Size(max = 255, message = "Ten qua toi da 255 ky tu")
    private String rewardName;

    private RewardType rewardType;

    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "Chi phi coin phai > 0")
    private BigDecimal coinCost;

    private String imageUrl;

    @Min(value = 0, message = "So luong ton kho phai >= 0")
    private Integer stockQuantity;

    private Boolean isUnlimited;

    private String termsConditions;
}
