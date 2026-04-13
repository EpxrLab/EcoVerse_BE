package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.RoundLeaderboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoundLeaderboardRepository extends JpaRepository<RoundLeaderboard, UUID> {

    List<RoundLeaderboard> findByCampaignIdOrderByCombinedAccuracyPercentageDescAvgTimeSecondsAsc(UUID campaignId);

    List<RoundLeaderboard> findByCampaignRoundIdOrderByCombinedAccuracyPercentageDescAvgTimeSecondsAsc(UUID campaignRoundId);

    // Tim entry cua 1 student trong 1 round
    Optional<RoundLeaderboard> findByCampaignRoundIdAndStudentId(UUID campaignRoundId, UUID studentId);

    boolean existsByCampaignRoundIdAndStudentIdAndIsAdvancedTrue(UUID campaignRoundId, UUID studentId);

    // Lay tat ca entry trong 1 round de re-rank
    List<RoundLeaderboard> findByCampaignRoundId(UUID campaignRoundId);

    // Lay tat ca entry cua campaign (tat ca cac round) - dung de tinh toan danh hieu
    List<RoundLeaderboard> findByCampaignId(UUID campaignId);

    // ── Report aggregate queries ───────────────────────────────────────────────

    @Query("SELECT MIN(rl.overallRankInRound) FROM RoundLeaderboard rl WHERE rl.student.id = :studentId AND rl.overallRankInRound IS NOT NULL")
    Integer findBestRankByStudentId(@Param("studentId") UUID studentId);

    @Query("SELECT AVG(rl.combinedAccuracyPercentage) FROM RoundLeaderboard rl WHERE rl.campaign.creatorPartnership.id = :partnershipId AND rl.combinedAccuracyPercentage IS NOT NULL")
    Double avgAccuracyByPartnershipId(@Param("partnershipId") UUID partnershipId);

    @Query("SELECT AVG(rl.combinedAccuracyPercentage) FROM RoundLeaderboard rl WHERE rl.campaign.id = :campaignId AND rl.school.id = :schoolId AND rl.combinedAccuracyPercentage IS NOT NULL")
    Double avgAccuracyByCampaignAndSchool(@Param("campaignId") UUID campaignId, @Param("schoolId") UUID schoolId);

    @Query("SELECT COUNT(DISTINCT rl.student.id) FROM RoundLeaderboard rl WHERE rl.campaign.id = :campaignId AND rl.school.id = :schoolId AND rl.isAdvanced = true")
    long countAdvancedStudentsByCampaignAndSchool(@Param("campaignId") UUID campaignId, @Param("schoolId") UUID schoolId);
}

