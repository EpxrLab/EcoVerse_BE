package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.RewardRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RewardRequestTrackingResponse {
    private UUID requestId;
    private String requestCode;
    private UUID studentId;
    private String studentName;
    private UUID rewardId;
    private String rewardName;
    private RewardRequestStatus currentStatus;
    private List<RewardRequestTrackingItemResponse> timeline;
}
