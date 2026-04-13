package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.SchoolLeaderboard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolLeaderboardRepository extends JpaRepository<SchoolLeaderboard, UUID> {

    List<SchoolLeaderboard> findByCampaignIdOrderByCombinedAccuracyPercentageDescAvgTimeSecondsAsc(UUID campaignId);

    // Tim entry cua 1 student trong 1 campaign
    Optional<SchoolLeaderboard> findByCampaignIdAndStudentId(UUID campaignId, UUID studentId);

    // Lay tat ca entry trong 1 campaign de re-rank
    List<SchoolLeaderboard> findByCampaignId(UUID campaignId);
}

