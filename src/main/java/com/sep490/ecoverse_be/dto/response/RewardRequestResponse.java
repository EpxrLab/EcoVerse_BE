package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.RewardRequestStatus;
import com.sep490.ecoverse_be.enums.RewardType;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RewardRequestResponse {

    private UUID id;
    private String requestCode;

    private UUID studentId;
    private String studentName;
    private String studentCode;

    private UUID rewardId;
    private String rewardName;
    private RewardType rewardType;
    private String rewardImageUrl;
    private String rewardImagePresignedUrl;

    private int quantity;
    private BigDecimal totalCoins;
    private RewardRequestStatus status;

    private String rejectedReason;
    private String cancelledReason;
    private UUID cancelledById;
    private String cancelledByName;
    private String notes;

    private String deliveryImageUrl;
    private String deliveryImagePresignedUrl;

    private UUID approvedBy;
    private OffsetDateTime approvedAt;
    private OffsetDateTime rejectedAt;
    private OffsetDateTime deliveredAt;
    private OffsetDateTime confirmedAt;
    private OffsetDateTime cancelledAt;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
