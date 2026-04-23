package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.Subscription;
import com.sep490.ecoverse_be.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID>,
        JpaSpecificationExecutor<Subscription> {

    Optional<Subscription> findBySubscriptionCode(String subscriptionCode);

    @Query("SELECT s FROM Subscription s WHERE s.school.id = :schoolId AND s.status = :status")
    Optional<Subscription> findBySchoolIdAndStatus(@Param("schoolId") UUID schoolId,
                                                   @Param("status") SubscriptionStatus status);

    @Query("SELECT s FROM Subscription s WHERE s.partnership.id = :partnershipId AND s.status = :status")
    Optional<Subscription> findByPartnershipIdAndStatus(@Param("partnershipId") UUID partnershipId,
                                                        @Param("status") SubscriptionStatus status);

    @Query("""
            SELECT s FROM Subscription s
            WHERE s.status = :status
            AND s.endDate BETWEEN :from AND :to
            """)
    List<Subscription> findByStatusAndEndDateBetween(@Param("status") SubscriptionStatus status,
                                                     @Param("from") OffsetDateTime from,
                                                     @Param("to") OffsetDateTime to);

    // ACTIVE đã quá endDate → chuyển PENDING_RENEWAL (đầu grace period)
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endDate < :now")
    List<Subscription> findExpiredSubscriptions(@Param("now") OffsetDateTime now);

    // PENDING_RENEWAL đã quá grace period → chuyển EXPIRED (lưu trữ)
    @Query("SELECT s FROM Subscription s WHERE s.status = 'PENDING_RENEWAL' AND s.endDate < :gracePeriodCutoff")
    List<Subscription> findOverdueRenewalSubscriptions(@Param("gracePeriodCutoff") LocalDateTime gracePeriodCutoff);

    // PENDING chờ thanh toán quá 24 giờ → chuyển CANCELLED
    @Query("SELECT s FROM Subscription s WHERE s.status = 'PENDING' AND s.createdAt < :cutoff")
    List<Subscription> findStalePendingSubscriptions(@Param("cutoff") LocalDateTime cutoff);

    // ── Report aggregate queries ───────────────────────────────────────────────

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.status = :status")
    long countByStatus(@Param("status") SubscriptionStatus status);

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.status = :status AND s.subscriberType = :type")
    long countByStatusAndSubscriberType(@Param("status") SubscriptionStatus status, @Param("type") com.sep490.ecoverse_be.enums.SubscriberType type);

    @Query("SELECT s FROM Subscription s WHERE s.school.id = :schoolId ORDER BY s.createdAt DESC")
    List<Subscription> findBySchoolIdOrderByCreatedAtDesc(@Param("schoolId") UUID schoolId);

    @Query("SELECT s FROM Subscription s WHERE s.partnership.id = :partnershipId ORDER BY s.createdAt DESC")
    List<Subscription> findByPartnershipIdOrderByCreatedAtDesc(@Param("partnershipId") UUID partnershipId);
}
