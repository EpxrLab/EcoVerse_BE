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

    List<CampaignParticipant> findByCampaignIdAndIsActiveTrueOrderByCreatedAtAsc(UUID campaignId);

    List<CampaignParticipant> findByStudentIdInAndIsActiveTrue(List<UUID> studentIds);

    List<CampaignParticipant> findByCampaignIdAndParentApprovalStatusNotAndIsActiveTrue(
            UUID campaignId, ParticipationStatus status);

    // Lay tat ca hoc sinh duoc moi (khong loc isActive) de hien thi toan bo trang thai
    List<CampaignParticipant> findByCampaignIdOrderByCreatedAtAsc(UUID campaignId);

    List<CampaignParticipant> findByCampaignIdAndInvitationSentAtIsNullAndIsActiveTrue(UUID campaignId);

    List<CampaignParticipant> findByCampaignIdAndSchoolIdAndIsActiveTrueOrderByCreatedAtAsc(
            UUID campaignId, UUID schoolId);

    List<CampaignParticipant> findByStudentIdInAndParentApprovalStatusAndIsActiveTrueAndInvitationSentAtIsNotNull(
            List<UUID> studentIds,
            ParticipationStatus status);

    // Lay tat ca participant theo parent_approval_status trong 1 campaign (dung cho auto-reject khi het han)
    List<CampaignParticipant> findByCampaignIdAndParentApprovalStatusAndIsActiveTrue(
            UUID campaignId, ParticipationStatus parentApprovalStatus);
}



