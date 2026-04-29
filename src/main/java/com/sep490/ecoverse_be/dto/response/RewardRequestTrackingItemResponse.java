package com.sep490.ecoverse_be.dto.response;

import com.sep490.ecoverse_be.enums.RewardRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RewardRequestTrackingItemResponse {
    private RewardRequestStatus status;
    private OffsetDateTime actionAt;
    private UUID actorId;
    private String actorName;
    private String reason;
}
