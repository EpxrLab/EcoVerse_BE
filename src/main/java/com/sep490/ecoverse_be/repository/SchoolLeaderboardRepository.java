package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.SchoolLeaderboard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SchoolLeaderboardRepository extends JpaRepository<SchoolLeaderboard, UUID> {

    List<SchoolLeaderboard> findByCampaignIdOrderByCombinedAccuracyPercentageDescAvgTimeSecondsAsc(UUID campaignId);
}

