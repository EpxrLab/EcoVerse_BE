package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.CampaignParticipant;
import com.sep490.ecoverse_be.enums.ParticipationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignParticipantRepository extends JpaRepository<CampaignParticipant, UUID> {

    long countByIsActiveTrue();

    List<CampaignParticipant> findByStudentIdAndIsActiveTrueOrderByCreatedAtDesc(UUID studentId);

    Optional<CampaignParticipant> findByCampaignIdAndStudentIdAndIsActiveTrue(UUID campaignId, UUID studentId);

    List<CampaignParticipant> findByStudentIdInAndParentApprovalStatusAndIsActiveTrue(
            List<UUID> studentIds,
            ParticipationStatus status
    );

    boolean existsByCampaignIdAndStudentId(UUID campaignId, UUID studentId);

    List<CampaignParticipant> findByStudentIdAndIsActiveTrue(UUID studentId);
}



