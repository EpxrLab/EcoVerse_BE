package com.sep490.ecoverse_be.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RewardStatusLogResponse {
    private UUID id;
    private String topic;
    private UUID referenceId;
    private String fromStatus;
    private String toStatus;
    private UUID actorUserId;
    private String actorName;
    private String actorRole;
    private String reason;
    private String notes;
    private OffsetDateTime transitionAt;
    private OffsetDateTime createdAt;
}
