package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.Subscription;
import com.sep490.ecoverse_be.enums.SubscriberType;
import com.sep490.ecoverse_be.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findBySubscriptionCode(String subscriptionCode);

    @Query("SELECT s FROM Subscription s WHERE s.school.id = :schoolId AND s.status = :status")
    Optional<Subscription> findBySchoolIdAndStatus(@Param("schoolId") UUID schoolId,
                                                   @Param("status") SubscriptionStatus status);

    @Query("SELECT s FROM Subscription s WHERE s.partnership.id = :partnershipId AND s.status = :status")
    Optional<Subscription> findByPartnershipIdAndStatus(@Param("partnershipId") UUID partnershipId,
                                                        @Param("status") SubscriptionStatus status);

    @Query("SELECT s FROM Subscription s WHERE s.school.id = :schoolId ORDER BY s.createdAt DESC")
    Page<Subscription> findBySchoolId(@Param("schoolId") UUID schoolId, Pageable pageable);

    @Query("SELECT s FROM Subscription s WHERE s.partnership.id = :partnershipId ORDER BY s.createdAt DESC")
    Page<Subscription> findByPartnershipId(@Param("partnershipId") UUID partnershipId, Pageable pageable);

    @Query("""
            SELECT s FROM Subscription s
            WHERE s.status = :status
            AND s.endDate BETWEEN :from AND :to
            """)
    List<Subscription> findByStatusAndEndDateBetween(@Param("status") SubscriptionStatus status,
                                                     @Param("from") LocalDateTime from,
                                                     @Param("to") LocalDateTime to);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endDate < :now")
    List<Subscription> findExpiredSubscriptions(@Param("now") LocalDateTime now);

    @Query("""
            SELECT s FROM Subscription s
            WHERE (:subscriberType IS NULL OR s.subscriberType = :subscriberType)
            AND (:status IS NULL OR s.status = :status)
            AND (:keyword IS NULL
                 OR LOWER(s.subscriptionCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(s.plan.planName) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY s.createdAt DESC
            """)
    Page<Subscription> findAllWithFilters(@Param("subscriberType") SubscriberType subscriberType,
                                          @Param("status") SubscriptionStatus status,
                                          @Param("keyword") String keyword,
                                          Pageable pageable);
}
