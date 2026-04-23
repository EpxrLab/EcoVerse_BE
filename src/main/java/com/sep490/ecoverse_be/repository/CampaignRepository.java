package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.Campaign;
import com.sep490.ecoverse_be.enums.CampaignType;
import com.sep490.ecoverse_be.enums.PartnershipCampaignStatus;
import com.sep490.ecoverse_be.enums.SchoolCampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
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
    List<Campaign> findSchoolCampaignsReadyForInviting(@Param("now") OffsetDateTime now);

    // Scheduler: INVITING/EXTENDED → ON_GOING khi đến startDate
    @Query("SELECT c FROM Campaign c WHERE c.campaignType = 'SCHOOL_INTERNAL' AND c.schoolStatus IN ('INVITING', 'EXTENDED') AND c.startDate <= :now AND c.isActive = true")
    List<Campaign> findSchoolCampaignsReadyForOnGoing(@Param("now") OffsetDateTime now);

    // Scheduler: ON_GOING → COMPLETED khi đến endDate
    @Query("SELECT c FROM Campaign c WHERE c.campaignType = 'SCHOOL_INTERNAL' AND c.schoolStatus = 'ON_GOING' AND c.endDate <= :now AND c.isActive = true")
    List<Campaign> findSchoolCampaignsReadyForCompleted(@Param("now") OffsetDateTime now);

    @Query("SELECT c FROM Campaign c WHERE c.campaignType = 'PARTNERSHIP_EVENT' AND c.partnershipStatus = 'SCHEDULED' AND c.registrationDate IS NOT NULL AND c.registrationDate <= :now AND c.isActive = true")
    List<Campaign> findPartnershipCampaignsReadyForJoining(@Param("now") OffsetDateTime now);

    @Query("SELECT c FROM Campaign c WHERE c.campaignType = 'PARTNERSHIP_EVENT' AND c.partnershipStatus = 'JOINING' AND c.invitationDate IS NOT NULL AND c.invitationDate <= :now AND c.isActive = true")
    List<Campaign> findPartnershipCampaignsReadyForInviting(@Param("now") OffsetDateTime now);

    @Query("SELECT c FROM Campaign c WHERE c.campaignType = 'PARTNERSHIP_EVENT' AND c.partnershipStatus = 'INVITING' AND c.startDate <= :now AND c.isActive = true")
    List<Campaign> findPartnershipCampaignsReadyForOnGoing(@Param("now") OffsetDateTime now);

    @Query("SELECT c FROM Campaign c WHERE c.campaignType = 'PARTNERSHIP_EVENT' AND c.partnershipStatus = 'ON_GOING' AND c.endDate <= :now AND c.isActive = true")
    List<Campaign> findPartnershipCampaignsReadyForCompleted(@Param("now") OffsetDateTime now);

    // ── Report aggregate queries ───────────────────────────────────────────────

    @Query("SELECT COUNT(c) FROM Campaign c WHERE c.creatorSchool.id = :schoolId AND c.isActive = true")
    long countByCreatorSchoolId(@Param("schoolId") UUID schoolId);

    @Query("SELECT COUNT(c) FROM Campaign c WHERE c.creatorSchool.id = :schoolId AND c.schoolStatus IN :statuses AND c.isActive = true")
    long countByCreatorSchoolIdAndSchoolStatusIn(@Param("schoolId") UUID schoolId, @Param("statuses") java.util.List<SchoolCampaignStatus> statuses);

    @Query("SELECT COUNT(c) FROM Campaign c WHERE c.creatorPartnership.id = :partnershipId AND c.isActive = true")
    long countByCreatorPartnershipId(@Param("partnershipId") UUID partnershipId);

    @Query("SELECT COUNT(c) FROM Campaign c WHERE c.creatorPartnership.id = :partnershipId AND c.partnershipStatus IN :statuses AND c.isActive = true")
    long countByCreatorPartnershipIdAndPartnershipStatusIn(@Param("partnershipId") UUID partnershipId, @Param("statuses") java.util.List<PartnershipCampaignStatus> statuses);

    @Query("SELECT COUNT(c) FROM Campaign c WHERE c.creatorSchool.id = :schoolId AND c.schoolStatus <> 'DRAFT' AND c.createdAt >= :startOfMonth")
    long countNonDraftByCreatorSchoolIdInMonth(@Param("schoolId") UUID schoolId, @Param("startOfMonth") OffsetDateTime startOfMonth);

    @Query("SELECT COUNT(c) FROM Campaign c WHERE c.creatorPartnership.id = :partnershipId AND c.partnershipStatus <> 'DRAFT' AND c.createdAt >= :startOfMonth")
    long countNonDraftByCreatorPartnershipIdInMonth(@Param("partnershipId") UUID partnershipId, @Param("startOfMonth") OffsetDateTime startOfMonth);

    @Query("SELECT COUNT(c) FROM Campaign c WHERE c.isActive = true AND (c.schoolStatus = 'ON_GOING' OR c.partnershipStatus = 'ON_GOING')")
    long countAllActiveCampaigns();
}


