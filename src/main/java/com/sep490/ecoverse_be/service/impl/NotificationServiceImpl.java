package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.response.NotificationResponse;
import com.sep490.ecoverse_be.entity.CampaignParticipant;
import com.sep490.ecoverse_be.entity.Notification;
import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.enums.NotificationStatus;
import com.sep490.ecoverse_be.enums.NotificationType;
import com.sep490.ecoverse_be.enums.Role;
import com.sep490.ecoverse_be.event.NotificationEvent;
import com.sep490.ecoverse_be.repository.NotificationRepository;
import com.sep490.ecoverse_be.repository.UserRepository;
import com.sep490.ecoverse_be.service.INotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final SimpMessagingTemplate messagingTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    // Dùng spring.mail.from thay vì username (SendGrid username la "apikey", không phải email)
    @Value("${spring.mail.from:}")
    private String fromEmail;

    // =====================================================================
    // Legacy methods (giu nguyen de khong lam hong PaymentServiceImpl, SubscriptionScheduler)
    // =====================================================================

    @Override
    @Transactional
    public void sendNotification(User recipient, NotificationType type, String title, String message) {
        sendNotification(recipient, type, title, message, null, null, null);
    }

    @Override
    @Transactional
    public void sendNotification(User recipient, NotificationType type, String title, String message,
                                 String referenceType, UUID referenceId, Map<String, Object> metadata) {
        // Luu vao DB
        Notification saved = saveNotification(recipient, type, title, message, referenceType, referenceId, null, metadata);
        // Push realtime qua WebSocket
        pushToUser(recipient.getId(), toResponse(saved));
        // Gui email async (kenh phu)
        sendEmailAsync(recipient.getEmail(), title, message);
        log.info("Notification sent to user {}: [{}] {}", recipient.getEmail(), type, title);
    }

    // =====================================================================
    // REST API methods
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(UUID userId, NotificationStatus status, Pageable pageable) {
        Page<Notification> page;
        if (status != null) {
            // Lọc theo trạng thái cụ thể
            page = notificationRepository.findByRecipientUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
        } else {
            // Lấy tất cả, sắp xếp mới nhất lên đầu
            page = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return page.map(this::toResponse);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        // Kiểm tra quyền: chỉ owner mới được đánh dấu
        if (!notification.getRecipientUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this notification");
        }

        // Chỉ cập nhật nếu đang là Unread
        if (NotificationStatus.UNREAD.equals(notification.getStatus())) {
            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(OffsetDateTime.now());
            notification = notificationRepository.save(notification);
        }
        return toResponse(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        // Bulk update bằng JPQL: hiệu quả hơn từng record
        notificationRepository.updateStatusByRecipientUserId(
                userId, NotificationStatus.UNREAD, NotificationStatus.READ);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByRecipientUserIdAndStatus(userId, NotificationStatus.UNREAD);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo"));
        if (!notification.getRecipientUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem thông báo này");
        }
        return toResponse(notification);
    }

    // =====================================================================
    // Core event-driven method
    // =====================================================================

    @Override
    @Transactional
    public void createAndPush(NotificationEvent event) {
        boolean isSingleUser = event.getRecipientUserId() != null;
        boolean isBroadcast = event.getRecipientUserIds() != null && !event.getRecipientUserIds().isEmpty();

        if (isSingleUser) {
            // Gửi cho 1 user cụ thể
            User recipient = userRepository.findById(event.getRecipientUserId())
                    .orElse(null);
            if (recipient == null) {
                log.warn("createAndPush: user not found with id={}", event.getRecipientUserId());
                return;
            }
            Notification saved = saveNotification(recipient, event.getType(), event.getTitle(), event.getMessage(),
                    event.getReferenceType(), event.getReferenceId(), event.getActionUrl(), event.getMetadata());
            pushToUser(recipient.getId(), toResponse(saved));
            if (event.isSendEmail()) {
                sendEmailAsync(recipient.getEmail(), event.getTitle(), event.getMessage());
            }

        } else if (isBroadcast) {
            // Gửi cho danh sách user ids cụ thể
            List<User> recipients = userRepository.findAllById(event.getRecipientUserIds());
            for (User recipient : recipients) {
                Notification saved = saveNotification(recipient, event.getType(), event.getTitle(), event.getMessage(),
                        event.getReferenceType(), event.getReferenceId(), event.getActionUrl(), event.getMetadata());
                pushToUser(recipient.getId(), toResponse(saved));
                if (event.isSendEmail()) {
                    sendEmailAsync(recipient.getEmail(), event.getTitle(), event.getMessage());
                }
            }
        } else {
            log.warn("createAndPush: event has no recipients defined, type={}", event.getType());
        }
    }

    @Override
    @Transactional
    public void createAndPushToUser(User recipient, NotificationType type, String title, String message,
                                    String referenceType, UUID referenceId, Map<String, Object> metadata,
                                    String actionUrl, boolean sendEmail) {
        Notification saved = saveNotification(recipient, type, title, message, referenceType, referenceId, actionUrl, metadata);
        pushToUser(recipient.getId(), toResponse(saved));
        if (sendEmail) {
            sendEmailAsync(recipient.getEmail(), title, message);
        }
    }

    // =====================================================================
    // Broadcast methods
    // =====================================================================

    @Override
    @Transactional
    public void notifyByRole(Role role, NotificationType type, String title, String message,
                             String referenceType, UUID referenceId, Map<String, Object> metadata) {
        // Query tất cả user hoạt động theo role bằng JPQL
        List<User> users = entityManager.createQuery(
                        "SELECT u FROM User u WHERE u.role = :role AND u.isActive = true AND u.status = 'ACTIVE'",
                        User.class)
                .setParameter("role", role)
                .getResultList();
        notifyUsers(users, type, title, message, referenceType, referenceId, metadata, false);
        log.info("Broadcast notification to role={}: {} users notified", role, users.size());
    }

    @Override
    @Transactional
    public void notifyBySchool(UUID schoolId, NotificationType type, String title, String message,
                               String referenceType, UUID referenceId, Map<String, Object> metadata) {
        // Lấy tất cả student thuộc trường, fan-out notification
        List<User> users = entityManager.createQuery(
                        "SELECT s.user FROM Student s WHERE s.school.id = :schoolId AND s.user.isActive = true",
                        User.class)
                .setParameter("schoolId", schoolId)
                .getResultList();
        notifyUsers(users, type, title, message, referenceType, referenceId, metadata, false);
        log.info("Broadcast notification to schoolId={}: {} users notified", schoolId, users.size());
    }

    @Override
    @Transactional
    public void notifyCampaignParticipants(UUID campaignId, NotificationType type, String title, String message,
                                           String referenceType, UUID referenceId, Map<String, Object> metadata) {
        // Lấy tất cả student đang tham gia campaign và Push cho User của họ
        List<CampaignParticipant> participants = entityManager.createQuery(
                        "SELECT cp FROM CampaignParticipant cp " +
                                "JOIN FETCH cp.student s " +
                                "JOIN FETCH s.user u " +
                                "WHERE cp.campaign.id = :campaignId AND cp.isActive = true",
                        CampaignParticipant.class)
                .setParameter("campaignId", campaignId)
                .getResultList();

        List<User> users = participants.stream()
                .map(cp -> cp.getStudent().getUser())
                .distinct()
                .toList();
        notifyUsers(users, type, title, message, referenceType, referenceId, metadata, false);
        log.info("Broadcast notification to campaign participants, campaignId={}: {} users notified", campaignId, users.size());
    }

    @Override
    @Transactional
    public void notifyCampaignParents(UUID campaignId, NotificationType type, String title, String message,
                                      String referenceType, UUID referenceId, Map<String, Object> metadata) {
        // Lay tat ca parent cua student dang tham gia campaign qua StudentParentLink
        List<User> parentUsers = entityManager.createQuery(
                        "SELECT DISTINCT spl.parent.user FROM StudentParentLink spl " +
                                "WHERE spl.student.id IN (" +
                                "  SELECT cp.student.id FROM CampaignParticipant cp " +
                                "  WHERE cp.campaign.id = :campaignId AND cp.isActive = true" +
                                ") AND spl.parent.user.isActive = true",
                        User.class)
                .setParameter("campaignId", campaignId)
                .getResultList();
        notifyUsers(parentUsers, type, title, message, referenceType, referenceId, metadata, false);
        log.info("Broadcast notification to campaign parents, campaignId={}: {} parents notified", campaignId, parentUsers.size());
    }

    @Override
    @Transactional
    public void notifyUsers(List<User> recipients, NotificationType type, String title, String message,
                            String referenceType, UUID referenceId, Map<String, Object> metadata, boolean sendEmail) {
        // Fan-out at write: mỗi User có 1 record riêng trong DB
        for (User recipient : recipients) {
            Notification saved = saveNotification(recipient, type, title, message,
                    referenceType, referenceId, null, metadata);
            pushToUser(recipient.getId(), toResponse(saved));
            if (sendEmail) {
                sendEmailAsync(recipient.getEmail(), title, message);
            }
        }
    }

    // =====================================================================
    // Private helpers
    // =====================================================================

    // Lưu Notification vào DB và trả về entity đã có id
    private Notification saveNotification(User recipient, NotificationType type, String title, String message,
                                          String referenceType, UUID referenceId, String actionUrl,
                                          Map<String, Object> metadata) {
        Notification notification = new Notification();
        notification.setRecipientUser(recipient);
        notification.setNotificationType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setStatus(NotificationStatus.UNREAD);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setActionUrl(actionUrl);
        notification.setMetadata(metadata);
        return notificationRepository.save(notification);
    }

    // Push notification qua WebSocket đến user-specific queue
    private void pushToUser(UUID userId, NotificationResponse response) {
        try {
            // SimpMessagingTemplate.convertAndSendToUser dùng Principal.getName() để định tuyến
            // Principal.getName() được set là userId.toString() trong WebSocketAuthInterceptor
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    response
            );
        } catch (Exception e) {
            // Neu user offline / chua connect WS thi chi log, khong throw (da co fallback REST)
            log.debug("WebSocket push skipped (user may be offline): userId={}, error={}", userId, e.getMessage());
        }
    }

    // Chuyển entity sang DTO để trả về cho client
    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getNotificationType() != null ? n.getNotificationType().name() : null,
                n.getTitle(),
                n.getMessage(),
                n.getStatus() != null ? n.getStatus().name() : null,
                n.getReferenceType(),
                n.getReferenceId(),
                n.getActionUrl(),
                n.getMetadata(),
                n.getReadAt(),
                n.getCreatedAt()
        );
    }

    // =====================================================================
    // Email (kênh phụ, async)
    // =====================================================================

    @Async
    public void sendEmailAsync(String toEmail, String subject, String bodyText) {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("Mail username not configured, skipping email to {}", toEmail);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[EcoVerse] " + subject);
            helper.setText(buildHtmlEmail(subject, bodyText), true);

            mailSender.send(mimeMessage);
            log.info("Email sent to {}: {}", toEmail, subject);

        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildHtmlEmail(String title, String message) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 30px auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                        .header { background-color: #2e7d32; color: #ffffff; padding: 20px 30px; text-align: center; }
                        .header h1 { margin: 0; font-size: 22px; }
                        .body { padding: 30px; color: #333333; line-height: 1.6; }
                        .body h2 { color: #2e7d32; margin-top: 0; }
                        .footer { padding: 15px 30px; background-color: #f9f9f9; text-align: center; font-size: 12px; color: #999999; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>EcoVerse</h1>
                        </div>
                        <div class="body">
                            <h2>%s</h2>
                            <p>%s</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2025 EcoVerse. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(title, message);
    }
}
