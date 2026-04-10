package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.Campaign;
import com.sep490.ecoverse_be.enums.CampaignType;
import com.sep490.ecoverse_be.enums.PartnershipCampaignStatus;
import com.sep490.ecoverse_be.enums.SchoolCampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    List<Campaign> findByCreatorSchoolIdAndIsActiveTrueOrderByCreatedAtDesc(UUID schoolId);

    List<Campaign> findByCreatorPartnershipIdAndIsActiveTrueOrderByCreatedAtDesc(UUID partnershipId);

    long countByCampaignType(CampaignType campaignType);

    long countBySchoolStatus(SchoolCampaignStatus status);

    long countByPartnershipStatus(PartnershipCampaignStatus status);

    // Scheduler: SCHEDULED → INVITING khi đến invitationDate
    @Query("SELECT c FROM Campaign c WHERE c.campaignType = 'SCHOOL_INTERNAL' AND c.schoolStatus = 'SCHEDULED' AND c.invitationDate IS NOT NULL AND c.invitationDate <= :now AND c.isActive = true")
    List<Campaign> findSchoolCampaignsReadyForInviting(@Param("now") LocalDateTime now);

    // Scheduler: INVITING/EXTENDED → ON_GOING khi đến startDate
    @Query("SELECT c FROM Campaign c WHERE c.campaignType = 'SCHOOL_INTERNAL' AND c.schoolStatus IN ('INVITING', 'EXTENDED') AND c.startDate <= :now AND c.isActive = true")
    List<Campaign> findSchoolCampaignsReadyForOnGoing(@Param("now") LocalDateTime now);

    // Scheduler: ON_GOING → COMPLETED khi đến endDate
    @Query("SELECT c FROM Campaign c WHERE c.campaignType = 'SCHOOL_INTERNAL' AND c.schoolStatus = 'ON_GOING' AND c.endDate <= :now AND c.isActive = true")
    List<Campaign> findSchoolCampaignsReadyForCompleted(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Campaign c WHERE c.campaignType = 'PARTNERSHIP_EVENT' AND c.partnershipStatus = 'SCHEDULED' AND c.registrationDate IS NOT NULL AND c.registrationDate <= :now AND c.isActive = true")
    List<Campaign> findPartnershipCampaignsReadyForJoining(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Campaign c WHERE c.campaignType = 'PARTNERSHIP_EVENT' AND c.partnershipStatus = 'JOINING' AND c.invitationDate IS NOT NULL AND c.invitationDate <= :now AND c.isActive = true")
    List<Campaign> findPartnershipCampaignsReadyForInviting(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Campaign c WHERE c.campaignType = 'PARTNERSHIP_EVENT' AND c.partnershipStatus = 'INVITING' AND c.startDate <= :now AND c.isActive = true")
    List<Campaign> findPartnershipCampaignsReadyForOnGoing(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Campaign c WHERE c.campaignType = 'PARTNERSHIP_EVENT' AND c.partnershipStatus = 'ON_GOING' AND c.endDate <= :now AND c.isActive = true")
    List<Campaign> findPartnershipCampaignsReadyForCompleted(@Param("now") LocalDateTime now);
}


