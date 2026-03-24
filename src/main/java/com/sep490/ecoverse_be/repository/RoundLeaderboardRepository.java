package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.RoundLeaderboard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoundLeaderboardRepository extends JpaRepository<RoundLeaderboard, UUID> {

    List<RoundLeaderboard> findByCampaignIdOrderByCombinedAccuracyPercentageDescAvgTimeSecondsAsc(UUID campaignId);

    List<RoundLeaderboard> findByCampaignRoundIdOrderByCombinedAccuracyPercentageDescAvgTimeSecondsAsc(UUID campaignRoundId);
}

