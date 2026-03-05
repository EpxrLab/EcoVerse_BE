package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.entity.Notification;
import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.enums.NotificationStatus;
import com.sep490.ecoverse_be.enums.NotificationType;
import com.sep490.ecoverse_be.repository.NotificationRepository;
import com.sep490.ecoverse_be.service.INotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Override
    @Transactional
    public void sendNotification(User recipient, NotificationType type, String title, String message) {
        sendNotification(recipient, type, title, message, null, null, null);
    }

    @Override
    @Transactional
    public void sendNotification(User recipient, NotificationType type, String title, String message,
                                 String referenceType, Long referenceId, Map<String, Object> metadata) {
        // 1. Save in-app notification to DB
        Notification notification = new Notification();
        notification.setRecipientUser(recipient);
        notification.setNotificationType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setStatus(NotificationStatus.UNREAD);
        notification.setReferenceType(referenceType);
        notification.setMetadata(metadata);

        notificationRepository.save(notification);
        log.info("Notification saved for user {}: [{}] {}", recipient.getEmail(), type, title);

        // 2. Send email asynchronously
        sendEmailAsync(recipient.getEmail(), title, message);
    }

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
