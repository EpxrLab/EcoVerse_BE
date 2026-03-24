package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.CampaignSchoolParticipate;
import com.sep490.ecoverse_be.enums.ParticipationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignSchoolParticipateRepository extends JpaRepository<CampaignSchoolParticipate, UUID> {

    long countByStatus(ParticipationStatus status);

    List<CampaignSchoolParticipate> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId);

    Optional<CampaignSchoolParticipate> findByIdAndSchoolId(UUID id, UUID schoolId);

    Optional<CampaignSchoolParticipate> findByCampaignIdAndSchoolId(UUID campaignId, UUID schoolId);

    List<CampaignSchoolParticipate> findByCampaignIdAndStatus(UUID campaignId, ParticipationStatus status);
}


