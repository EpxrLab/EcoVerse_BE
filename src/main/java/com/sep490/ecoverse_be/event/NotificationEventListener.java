package com.sep490.ecoverse_be.event;

import com.sep490.ecoverse_be.service.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

// Listener nhận NotificationEvent từ ApplicationEventPublisher
// @Async: xử lý bất đồng bộ, không block luồng gởi của business service
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final INotificationService notificationService;

    // Nhận bất kỳ NotificationEvent nào được publish trong hệ thống
    @Async
    @EventListener
    public void handleNotificationEvent(NotificationEvent event) {
        log.debug("Received NotificationEvent: type={}, title={}", event.getType(), event.getTitle());

        try {
            // Delegate toàn bộ xử lý xuống NotificationService
            notificationService.createAndPush(event);
        } catch (Exception e) {
            log.error("Failed to process NotificationEvent: type={}, error={}", event.getType(), e.getMessage(), e);
        }
    }
}
