package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.PartnershipRewardStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class CampaignRewardDeliveryResponse {

    private UUID id;

    // Campaign info
    private UUID campaignId;
    private String campaignName;

    // Reward info
    private UUID campaignRewardId;
    private String rewardName;
    private String rewardImageUrl;
    private String rewardImagePresignedUrl;
    private Integer rankPosition;

    // Student info
    private UUID studentId;
    private String studentName;
    private String studentCode;

    // School info
    private UUID schoolId;
    private String schoolName;
    private String schoolAddress;
    private String schoolWard;
    private String schoolProvince;

    // Ranking & score
    private Integer leaderboardRank;
    private BigDecimal totalScore;

    // Delivery status
    private PartnershipRewardStatus status;

    // Timestamps
    private OffsetDateTime preparingAt;
    private OffsetDateTime shippedAt;
    private OffsetDateTime arrivedAt;
    private OffsetDateTime deliveredAt;
    private OffsetDateTime confirmedAt;

    // Shipping
    private String shippingTrackingCode;

    // Delivery proof image
    private String deliveryImageUrl;
    private String deliveryImagePresignedUrl;

    // Actor names for audit trail
    private String arrivedConfirmedByName;
    private String deliveredByName;
    private String confirmedByName;

    private String notes;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
