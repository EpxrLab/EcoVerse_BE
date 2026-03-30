package com.sep490.ecoverse_be.dto.response;

import java.time.LocalDateTime;
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
        LocalDateTime readAt,
        LocalDateTime createdAt
) {}
