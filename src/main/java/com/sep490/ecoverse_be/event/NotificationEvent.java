package com.sep490.ecoverse_be.event;

import com.sep490.ecoverse_be.enums.NotificationType;
import lombok.Builder;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// Spring Application Event dùng để mang dữ liệu tạo và gửi (push) thông báo
// Sử dụng @Builder để các Service publish event một cách rõ ràng, tránh hardcode
@Getter
public class NotificationEvent extends ApplicationEvent {

    // Loại thông báo (CAMPAIGN_START, REWARD_DELIVERED, ...)
    private final NotificationType type;

    // Tiêu đề và nội dung thông báo
    private final String title;
    private final String message;

    // Dùng khi gửi cho một user cụ thể (null nếu là broadcast)
    private final UUID recipientUserId;

    // Dùng khi gửi cho nhiều user (broadcast) (null nếu là gửi đơn lẻ)
    private final List<UUID> recipientUserIds;

    // Thông tin tham chiếu đến đối tượng liên quan (Campaign, Reward, ...)
    private final String referenceType;
    private final UUID referenceId;

    // Dữ liệu bổ sung (dạng JSON)
    private final Map<String, Object> metadata;

    // URL để frontend điều hướng khi người dùng nhấn vào thông báo
    private final String actionUrl;

    // Có gửi email hay không (email là kênh phụ, WebSocket là kênh chính)
    private final boolean sendEmail;

    @Builder
    public NotificationEvent(
            Object source,
            NotificationType type,
            String title,
            String message,
            UUID recipientUserId,
            List<UUID> recipientUserIds,
            String referenceType,
            UUID referenceId,
            Map<String, Object> metadata,
            String actionUrl,
            boolean sendEmail) {

        super(source != null ? source : "NotificationEvent");

        this.type = type;
        this.title = title;
        this.message = message;
        this.recipientUserId = recipientUserId;
        this.recipientUserIds = recipientUserIds;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.metadata = metadata;
        this.actionUrl = actionUrl;
        this.sendEmail = sendEmail;
    }
}