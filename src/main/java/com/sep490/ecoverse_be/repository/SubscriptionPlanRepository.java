package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.SubscriptionPlan;
import com.sep490.ecoverse_be.enums.SubscriberType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID>,
        JpaSpecificationExecutor<SubscriptionPlan> {

    Optional<SubscriptionPlan> findByPlanCode(String planCode);

    boolean existsByPlanCode(String planCode);

    boolean existsByPlanName(String planName);

    @Query("SELECT COUNT(s) > 0 FROM SubscriptionPlan s WHERE s.planName = :planName AND s.id <> :id")
    boolean existsByPlanNameAndIdNot(@Param("planName") String planName, @Param("id") UUID id);

    @Query("""
            SELECT p FROM SubscriptionPlan p
            WHERE p.subscriberType = :type
              AND p.price = 0
              AND p.isActive = true
            ORDER BY p.displayOrder ASC
            LIMIT 1
            """)
    Optional<SubscriptionPlan> findActiveFreeBySubscriberType(@Param("type") SubscriberType type);
}
