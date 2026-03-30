package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.Notification;
import com.sep490.ecoverse_be.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Lay toan bo notification cua user, sap xep moi nhat len dau
    Page<Notification> findByRecipientUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Lay notification cua user theo trang thai (UNREAD / READ / ARCHIVED)
    Page<Notification> findByRecipientUserIdAndStatusOrderByCreatedAtDesc(
            UUID userId, NotificationStatus status, Pageable pageable);

    // Dem so notification chua doc cua user
    long countByRecipientUserIdAndStatus(UUID userId, NotificationStatus status);

    // Danh dau tat ca UNREAD -> READ cho 1 user (bulk update)
    @Modifying
    @Query("UPDATE Notification n SET n.status = :newStatus WHERE n.recipientUser.id = :userId AND n.status = :oldStatus")
    int updateStatusByRecipientUserId(
            @Param("userId") UUID userId,
            @Param("oldStatus") NotificationStatus oldStatus,
            @Param("newStatus") NotificationStatus newStatus);
}
