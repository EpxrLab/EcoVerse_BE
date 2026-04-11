package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.response.NotificationResponse;
import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.enums.NotificationStatus;
import com.sep490.ecoverse_be.enums.NotificationType;
import com.sep490.ecoverse_be.enums.Role;
import com.sep490.ecoverse_be.event.NotificationEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface INotificationService {

    void sendNotification(User recipient, NotificationType type, String title, String message);

    void sendNotification(User recipient, NotificationType type, String title, String message,
                          String referenceType, UUID referenceId, Map<String, Object> metadata);


    // Lấy danh sách thông báo của người dùng (có phân trang, lọc theo trạng thái)
    Page<NotificationResponse> getNotifications(UUID userId, NotificationStatus status, Pageable pageable);

    // Đánh dấu một thông báo là đã đọc
    NotificationResponse markAsRead(UUID notificationId, UUID userId);

    // Đánh dấu tất cả thông báo UNREAD thành READ cho người dùng
    void markAllAsRead(UUID userId);

    // Đếm số lượng thông báo UNREAD của người dùng
    long getUnreadCount(UUID userId);

    // Lấy chi tiết 1 thông báo theo ID (kiểm tra quyền truy cập)
    NotificationResponse getNotificationById(UUID notificationId, UUID userId);

    // ---- Core event-driven methods ----

    // Tạo thông báo từ event và gửi realtime qua WebSocket
    void createAndPush(NotificationEvent event);

    // Tạo và gửi thông báo cho một người dùng cụ thể (đầy đủ tham số)
    void createAndPushToUser(User recipient, NotificationType type, String title, String message,
                             String referenceType, UUID referenceId, Map<String, Object> metadata,
                             String actionUrl, boolean sendEmail);

    // ---- Broadcast methods ----

    // Gửi thông báo cho tất cả người dùng theo role (ví dụ: SYSTEM_ANNOUNCEMENT cho admin)
    void notifyByRole(Role role, NotificationType type, String title, String message,
                      String referenceType, UUID referenceId, Map<String, Object> metadata);

    // Gửi thông báo cho tất cả học sinh trong một trường
    void notifyBySchool(UUID schoolId, NotificationType type, String title, String message,
                        String referenceType, UUID referenceId, Map<String, Object> metadata);

    // Gửi thông báo cho tất cả người tham gia một campaign (học sinh)
    void notifyCampaignParticipants(UUID campaignId, NotificationType type, String title, String message,
                                    String referenceType, UUID referenceId, Map<String, Object> metadata);

    // Gửi thông báo cho tất cả phụ huynh của học sinh tham gia campaign
    void notifyCampaignParents(UUID campaignId, NotificationType type, String title, String message,
                               String referenceType, UUID referenceId, Map<String, Object> metadata);

    // Gửi thông báo cho danh sách người dùng cụ thể (broadcast linh hoạt)
    void notifyUsers(List<User> recipients, NotificationType type, String title, String message,
                     String referenceType, UUID referenceId, Map<String, Object> metadata, boolean sendEmail);
}
