package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.CampaignRoundParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignRoundParticipantRepository extends JpaRepository<CampaignRoundParticipant, UUID> {

    int countByCampaignParticipantIdAndCompletedAtIsNotNull(UUID campaignParticipantId);

    Optional<CampaignRoundParticipant> findByCampaignRoundIdAndCampaignParticipantId(UUID campaignRoundId, UUID campaignParticipantId);

    boolean existsByCampaignRoundIdAndCampaignParticipantIdAndIsAdvancedTrue(UUID campaignRoundId, UUID campaignParticipantId);

    List<CampaignRoundParticipant> findByCampaignRoundId(UUID campaignRoundId);

    @Query("SELECT crp.campaignParticipant.student.id FROM CampaignRoundParticipant crp WHERE crp.campaignRound.id = :campaignRoundId")
    List<UUID> findStudentIdsByCampaignRoundId(@Param("campaignRoundId") UUID campaignRoundId);
}

