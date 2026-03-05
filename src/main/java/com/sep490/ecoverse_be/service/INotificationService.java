package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.enums.NotificationType;

import java.util.Map;

public interface INotificationService {

    void sendNotification(User recipient, NotificationType type, String title, String message);

    void sendNotification(User recipient, NotificationType type, String title, String message,
                          String referenceType, Long referenceId, Map<String, Object> metadata);
}
