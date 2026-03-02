package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.SubscriptionPlan;
import com.sep490.ecoverse_be.enums.SubscriberType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    Optional<SubscriptionPlan> findByPlanCode(String planCode);

    boolean existsByPlanCode(String planCode);

    boolean existsByPlanName(String planName);

    @Query("SELECT COUNT(s) > 0 FROM SubscriptionPlan s WHERE s.planName = :planName AND s.id <> :id")
    boolean existsByPlanNameAndIdNot(@Param("planName") String planName, @Param("id") Long id);

    @Query("""
            SELECT sp FROM SubscriptionPlan sp
            WHERE (:subscriberType IS NULL OR sp.subscriberType = :subscriberType)
            AND (:isActive IS NULL OR sp.isActive = :isActive)
            AND (:keyword IS NULL OR LOWER(sp.planName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(sp.planCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<SubscriptionPlan> findAllWithFilters(
            @Param("subscriberType") SubscriberType subscriberType,
            @Param("isActive") Boolean isActive,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
