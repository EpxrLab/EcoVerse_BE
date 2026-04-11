package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.RoundLeaderboard;
import org.springframework.data.jpa.repository.JpaRepository;

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
}

