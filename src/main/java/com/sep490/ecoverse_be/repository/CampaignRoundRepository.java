package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.CampaignRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignRoundRepository extends JpaRepository<CampaignRound, UUID> {

    List<CampaignRound> findByCampaignIdOrderByRoundNumberAsc(UUID campaignId);

    Optional<CampaignRound> findByIdAndCampaignId(UUID id, UUID campaignId);
}

