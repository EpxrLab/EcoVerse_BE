package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface GameSessionRepository extends JpaRepository<GameSession, UUID> {

    @Query("""
            SELECT gs
            FROM GameSession gs
            WHERE gs.campaignParticipant.id = :participantId
              AND gs.roundGameConfig.campaignRound.id = :roundId
            ORDER BY gs.createdAt DESC
            """)
    List<GameSession> findByParticipantAndRound(@Param("participantId") UUID participantId,
                                                @Param("roundId") UUID roundId);

    @Query("""
            SELECT gs
            FROM GameSession gs
            WHERE gs.campaignParticipant.id = :participantId
              AND gs.roundGameConfig.campaignRound.id = :roundId
              AND gs.isCompleted = true
            """)
    List<GameSession> findCompletedByParticipantAndRound(@Param("participantId") UUID participantId,
                                                         @Param("roundId") UUID roundId);

    @Query("""
            SELECT gs
            FROM GameSession gs
            WHERE gs.campaignParticipant.id = :participantId
              AND gs.roundGameConfig.campaignRound.campaign.id = :campaignId
              AND gs.isCompleted = true
            """)
    List<GameSession> findCompletedByParticipantAndCampaign(@Param("participantId") UUID participantId,
                                                            @Param("campaignId") UUID campaignId);

    @Query("""
            SELECT gs
            FROM GameSession gs
            WHERE gs.campaignParticipant.id = :participantId
              AND gs.roundGameConfig.id = :roundGameConfigId
              AND gs.isCompleted = false
            """)
    Optional<GameSession> findOpenSessionByParticipantAndConfig(@Param("participantId") UUID participantId,
                                                                @Param("roundGameConfigId") UUID roundGameConfigId);

        boolean existsByCampaignParticipantIdAndRoundGameConfigIdAndGameLevelPresetIdAndCurrentLevelAndIsCompletedTrueAndIsPassedTrue(
          UUID participantId,
          UUID roundGameConfigId,
          UUID gameLevelPresetId,
          int currentLevel
        );

        long countByCampaignParticipantIdAndRoundGameConfigIdAndGameLevelPresetIdAndCurrentLevelAndSessionStartBetween(
          UUID participantId,
          UUID roundGameConfigId,
          UUID gameLevelPresetId,
          int currentLevel,
          LocalDateTime from,
          LocalDateTime to
        );

        boolean existsByCampaignParticipantIdAndRoundGameConfigIdAndGameLevelPresetIdAndCurrentLevelAndCoinAwardedIsNotNullAndCoinAwardedGreaterThan(
          UUID participantId,
          UUID roundGameConfigId,
          UUID gameLevelPresetId,
          int currentLevel,
          Integer minCoin
        );

            boolean existsByCampaignParticipantIdAndRoundGameConfigIdAndGameLevelPresetIdAndCurrentLevelAndCoinAwardedGreaterThan(
              UUID participantId,
              UUID roundGameConfigId,
              UUID gameLevelPresetId,
              int currentLevel,
              Integer minCoin
            );

    @Query("""
            SELECT gs
            FROM GameSession gs
            WHERE gs.campaignParticipant.student.id = :studentId
              AND gs.isCompleted = false
            ORDER BY gs.sessionStart ASC
            """)
    List<GameSession> findAllOpenSessionsByStudentId(@Param("studentId") UUID studentId);

    // ── Report aggregate queries ───────────────────────────────────────────────

    @Query("SELECT COUNT(gs) FROM GameSession gs WHERE gs.campaignParticipant.student.id = :studentId AND gs.isCompleted = true")
    long countCompletedByStudentId(@Param("studentId") UUID studentId);

    @Query("SELECT AVG(gs.accuracyPercentage) FROM GameSession gs WHERE gs.campaignParticipant.student.id = :studentId AND gs.isCompleted = true")
    Double avgAccuracyByStudentId(@Param("studentId") UUID studentId);

    @Query("SELECT MAX(gs.accuracyPercentage) FROM GameSession gs WHERE gs.campaignParticipant.student.id = :studentId AND gs.isCompleted = true")
    java.math.BigDecimal maxAccuracyByStudentId(@Param("studentId") UUID studentId);

    @Query("SELECT COUNT(gs) FROM GameSession gs WHERE gs.campaignParticipant.student.id = :studentId AND gs.isCompleted = true AND gs.createdAt BETWEEN :from AND :to")
    long countCompletedByStudentIdAndDateRange(@Param("studentId") UUID studentId, @Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query("SELECT AVG(gs.accuracyPercentage) FROM GameSession gs WHERE gs.campaignParticipant.student.id = :studentId AND gs.isCompleted = true AND gs.createdAt BETWEEN :from AND :to")
    Double avgAccuracyByStudentIdAndDateRange(@Param("studentId") UUID studentId, @Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query("SELECT AVG(gs.accuracyPercentage) FROM GameSession gs WHERE gs.campaignParticipant.school.id = :schoolId AND gs.isCompleted = true")
    Double avgAccuracyBySchoolId(@Param("schoolId") UUID schoolId);

    @Query("SELECT AVG(gs.accuracyPercentage) FROM GameSession gs WHERE gs.campaignParticipant.campaign.creatorPartnership.id = :partnershipId AND gs.isCompleted = true")
    Double avgAccuracyByPartnershipId(@Param("partnershipId") UUID partnershipId);

    @Query("SELECT COUNT(gs) FROM GameSession gs WHERE gs.isCompleted = true")
    long countAllCompleted();

    @Query("SELECT gs.campaignParticipant.student.id, AVG(gs.accuracyPercentage) FROM GameSession gs WHERE gs.campaignParticipant.school.id = :schoolId AND gs.isCompleted = true GROUP BY gs.campaignParticipant.student.id ORDER BY AVG(gs.accuracyPercentage) DESC")
    List<Object[]> findTopStudentsByGameAccuracyInSchool(@Param("schoolId") UUID schoolId, org.springframework.data.domain.Pageable pageable);
}
