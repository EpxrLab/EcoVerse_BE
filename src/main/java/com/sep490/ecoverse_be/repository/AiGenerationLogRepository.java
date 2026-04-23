package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.AiGenerationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Repository
public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, UUID> {

    /**
     * Đếm số lần AI đã được gọi thành công cho School trong kỳ subscription.
     * Dùng để kiểm tra quota trước khi cho phép generate mới.
     */
    @Query("""
            SELECT COUNT(a) FROM AiGenerationLog a
            WHERE a.school.id = :schoolId
              AND a.isUsageCharged = true
              AND a.createdAt BETWEEN :start AND :end
            """)
    long countChargedBySchoolIdInPeriod(@Param("schoolId") UUID schoolId,
                                       @Param("start") OffsetDateTime start,
                                       @Param("end") OffsetDateTime end);

    /**
     * Đếm số lần AI đã được gọi thành công cho Partnership trong kỳ subscription.
     */
    @Query("""
            SELECT COUNT(a) FROM AiGenerationLog a
            WHERE a.partnership.id = :partnershipId
              AND a.isUsageCharged = true
              AND a.createdAt BETWEEN :start AND :end
            """)
    long countChargedByPartnershipIdInPeriod(@Param("partnershipId") UUID partnershipId,
                                            @Param("start") OffsetDateTime start,
                                            @Param("end") OffsetDateTime end);
}
