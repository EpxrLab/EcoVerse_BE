package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.CampaignSchoolParticipate;
import com.sep490.ecoverse_be.enums.ParticipationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignSchoolParticipateRepository extends JpaRepository<CampaignSchoolParticipate, UUID> {

    long countByStatus(ParticipationStatus status);

    List<CampaignSchoolParticipate> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId);

    Optional<CampaignSchoolParticipate> findByIdAndSchoolId(UUID id, UUID schoolId);

    Optional<CampaignSchoolParticipate> findByCampaignIdAndSchoolId(UUID campaignId, UUID schoolId);

    List<CampaignSchoolParticipate> findByCampaignIdAndStatus(UUID campaignId, ParticipationStatus status);

    List<CampaignSchoolParticipate> findByCampaignId(UUID campaignId);

    List<CampaignSchoolParticipate> findByCampaignIdAndInvitationSentAtIsNull(UUID campaignId);

    // ── Report aggregate queries ───────────────────────────────────────────────

    @Query("SELECT COUNT(DISTINCT csp.school.id) FROM CampaignSchoolParticipate csp WHERE csp.campaign.creatorPartnership.id = :partnershipId")
    long countDistinctSchoolsByPartnershipId(@Param("partnershipId") UUID partnershipId);

    @Query("SELECT SUM(csp.studentsEnrolled) FROM CampaignSchoolParticipate csp WHERE csp.campaign.creatorPartnership.id = :partnershipId")
    Long sumStudentsEnrolledByPartnershipId(@Param("partnershipId") UUID partnershipId);

    @Query("SELECT csp.school.id, csp.school.schoolName, SUM(csp.studentsEnrolled) as total FROM CampaignSchoolParticipate csp WHERE csp.campaign.creatorPartnership.id = :partnershipId GROUP BY csp.school.id, csp.school.schoolName ORDER BY total DESC")
    List<Object[]> findTopSchoolsByStudentsEnrolledForPartnership(@Param("partnershipId") UUID partnershipId, Pageable pageable);

    @Query("SELECT COUNT(csp) FROM CampaignSchoolParticipate csp WHERE csp.school.id = :schoolId AND csp.status = :status")
    long countBySchoolIdAndStatus(@Param("schoolId") UUID schoolId, @Param("status") ParticipationStatus status);

    @Query("SELECT COUNT(csp) FROM CampaignSchoolParticipate csp WHERE csp.school.id = :schoolId")
    long countBySchoolId(@Param("schoolId") UUID schoolId);

    @Query("SELECT SUM(csp.studentsEnrolled) FROM CampaignSchoolParticipate csp WHERE csp.campaign.id = :campaignId AND csp.school.id = :schoolId")
    Long sumStudentsEnrolledByCampaignAndSchool(@Param("campaignId") UUID campaignId, @Param("schoolId") UUID schoolId);
}


