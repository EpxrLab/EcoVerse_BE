package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.RoundGameConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoundGameConfigRepository extends JpaRepository<RoundGameConfig, UUID> {

    List<RoundGameConfig> findByCampaignRoundIdOrderByDisplayOrderAsc(UUID campaignRoundId);

    Optional<RoundGameConfig> findFirstByCampaignRoundIdOrderByDisplayOrderAsc(UUID campaignRoundId);

    Optional<RoundGameConfig> findByIdAndCampaignRoundId(UUID id, UUID campaignRoundId);
}

