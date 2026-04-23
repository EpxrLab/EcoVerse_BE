package com.sep490.ecoverse_be.dto.response;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

// DTO tra ve cho client: REST API va WebSocket push dung chung
public record NotificationResponse(
        UUID id,
        String notificationType,
        String title,
        String message,
        String status,
        String referenceType,
        UUID referenceId,
        String actionUrl,
        Map<String, Object> metadata,
        OffsetDateTime readAt,
        OffsetDateTime createdAt
) {}
