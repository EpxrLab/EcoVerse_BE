package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.enums.RewardRequestStatus;
import com.sep490.ecoverse_be.enums.RewardType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    private int quantity;
    private BigDecimal totalCoins;
    private RewardRequestStatus status;

    private String rejectedReason;
    private String cancelledReason;
    private String notes;

    private UUID approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
