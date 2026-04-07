package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.CampaignReward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignRewardRepository extends JpaRepository<CampaignReward, UUID> {

    List<CampaignReward> findByCampaignIdOrderByRankPositionAsc(UUID campaignId);

    void deleteByCampaignId(UUID campaignId);
    Optional<CampaignReward> findByCampaignIdAndRankPosition(UUID campaignId, Integer rankPosition);
}
