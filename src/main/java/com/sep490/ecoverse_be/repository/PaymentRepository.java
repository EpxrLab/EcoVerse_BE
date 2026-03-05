package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.Payment;
import com.sep490.ecoverse_be.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentCode(String paymentCode);

    Optional<Payment> findByTransactionRef(String transactionRef);

    @Query("SELECT p FROM Payment p WHERE p.subscription.id = :subscriptionId ORDER BY p.createdAt DESC")
    Page<Payment> findBySubscriptionId(@Param("subscriptionId") Long subscriptionId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.school.id = :schoolId ORDER BY p.createdAt DESC")
    Page<Payment> findBySchoolId(@Param("schoolId") Long schoolId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.partnership.id = :partnershipId ORDER BY p.createdAt DESC")
    Page<Payment> findByPartnershipId(@Param("partnershipId") Long partnershipId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.subscription.id = :subscriptionId AND p.status = :status")
    Optional<Payment> findBySubscriptionIdAndStatus(@Param("subscriptionId") Long subscriptionId,
                                                     @Param("status") PaymentStatus status);
}
