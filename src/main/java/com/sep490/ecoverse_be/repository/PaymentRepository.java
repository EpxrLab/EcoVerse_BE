package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.Payment;
import com.sep490.ecoverse_be.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByPaymentCode(String paymentCode);

    Optional<Payment> findByTransactionRef(String transactionRef);

    @Query("SELECT p FROM Payment p WHERE p.subscription.id = :subscriptionId ORDER BY p.createdAt DESC")
    Page<Payment> findBySubscriptionId(@Param("subscriptionId") UUID subscriptionId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.school.id = :schoolId ORDER BY p.createdAt DESC")
    Page<Payment> findBySchoolId(@Param("schoolId") UUID schoolId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.partnership.id = :partnershipId ORDER BY p.createdAt DESC")
    Page<Payment> findByPartnershipId(@Param("partnershipId") UUID partnershipId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.subscription.id = :subscriptionId AND p.status = :status")
    Optional<Payment> findBySubscriptionIdAndStatus(@Param("subscriptionId") UUID subscriptionId,
                                                     @Param("status") PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.subscription.id = :subscriptionId ORDER BY p.createdAt DESC")
    List<Payment> findAllBySubscriptionIdOrderByCreatedAtDesc(@Param("subscriptionId") UUID subscriptionId);

    @Query("SELECT p FROM Payment p WHERE p.subscription.id IN :subscriptionIds ORDER BY p.createdAt DESC")
    List<Payment> findAllBySubscriptionIdInOrderByCreatedAtDesc(@Param("subscriptionIds") List<UUID> subscriptionIds);

    // ── Report aggregate queries ───────────────────────────────────────────────

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = :status")
    java.math.BigDecimal sumAmountByStatus(@Param("status") com.sep490.ecoverse_be.enums.PaymentStatus status);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = :status AND p.paidAt BETWEEN :from AND :to")
    java.math.BigDecimal sumAmountByStatusAndDateRange(@Param("status") com.sep490.ecoverse_be.enums.PaymentStatus status, @Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.subscriberType = :type AND p.paidAt BETWEEN :from AND :to")
    java.math.BigDecimal sumAmountBySubscriberTypeAndDateRange(@Param("type") com.sep490.ecoverse_be.enums.SubscriberType type, @Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.subscriberType = :type")
    java.math.BigDecimal sumAmountBySubscriberType(@Param("type") com.sep490.ecoverse_be.enums.SubscriberType type);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status")
    long countByStatus(@Param("status") com.sep490.ecoverse_be.enums.PaymentStatus status);

    @Query(value = "SELECT EXTRACT(YEAR FROM paid_at) AS yr, EXTRACT(MONTH FROM paid_at) AS mo, SUM(amount) AS total FROM payments WHERE status = 'COMPLETED' AND paid_at >= :from GROUP BY yr, mo ORDER BY yr, mo", nativeQuery = true)
    List<Object[]> findMonthlyRevenueTrend(@Param("from") java.time.LocalDateTime from);
}
