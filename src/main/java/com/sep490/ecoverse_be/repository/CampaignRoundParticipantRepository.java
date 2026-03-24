package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.CampaignRoundParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CampaignRoundParticipantRepository extends JpaRepository<CampaignRoundParticipant, UUID> {

    int countByCampaignParticipantIdAndCompletedAtIsNotNull(UUID campaignParticipantId);
}

