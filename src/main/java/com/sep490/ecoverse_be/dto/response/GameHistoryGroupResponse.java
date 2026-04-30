package com.sep490.ecoverse_be.dto.response;

import lombok.Builder;
import java.util.UUID;
import java.util.List;

@Builder
public record GameHistoryGroupResponse(
        UUID roundGameConfigId,
        String gameTypeName,
        List<StudentGameSessionSummaryResponse> sessions
) {}
