package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.CampaignRewardDelivery;
import com.sep490.ecoverse_be.enums.PartnershipRewardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface CampaignRewardDeliveryRepository extends JpaRepository<CampaignRewardDelivery, UUID> {

    boolean existsByCampaignRewardIdAndStudentId(UUID campaignRewardId, UUID studentId);

    // Partnership xem tat ca deliveries cua campaign (co the loc theo status)
    List<CampaignRewardDelivery> findByCampaignIdOrderByLeaderboardRankAsc(UUID campaignId);

    List<CampaignRewardDelivery> findByCampaignIdAndStatusOrderByLeaderboardRankAsc(
            UUID campaignId, PartnershipRewardStatus status);

    // School xem deliveries cua truong minh trong campaign
    List<CampaignRewardDelivery> findBySchoolIdAndCampaignIdOrderByLeaderboardRankAsc(
            UUID schoolId, UUID campaignId);

    List<CampaignRewardDelivery> findBySchoolIdAndCampaignIdAndStatusOrderByLeaderboardRankAsc(
            UUID schoolId, UUID campaignId, PartnershipRewardStatus status);

    // Student/Parent xem qua cua minh
    List<CampaignRewardDelivery> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

    // Auto-confirm scheduler: tim cac DELIVERED qua deadline
    List<CampaignRewardDelivery> findByStatusAndDeliveredAtBefore(
            PartnershipRewardStatus status, OffsetDateTime deadline);

    // Tim tat ca deliveries theo campaign (cho partnership de get notifications)
    @Query("SELECT d FROM CampaignRewardDelivery d WHERE d.campaign.id = :campaignId AND d.campaign.creatorPartnership.id = :partnershipId ORDER BY d.leaderboardRank ASC")
    List<CampaignRewardDelivery> findByCampaignIdAndPartnershipId(
            @Param("campaignId") UUID campaignId,
            @Param("partnershipId") UUID partnershipId);

    // Lay tat ca deliveries thuoc campaigns cua 1 partnership (cho status log)
    @Query("SELECT d FROM CampaignRewardDelivery d WHERE d.campaign.creatorPartnership.id = :partnershipId")
    List<CampaignRewardDelivery> findByPartnershipId(@Param("partnershipId") UUID partnershipId);
}
