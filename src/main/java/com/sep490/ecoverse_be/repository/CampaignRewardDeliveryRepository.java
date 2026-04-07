package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.CampaignRewardDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CampaignRewardDeliveryRepository extends JpaRepository<CampaignRewardDelivery, UUID> {

    boolean existsByCampaignRewardIdAndStudentId(UUID campaignRewardId, UUID studentId);
}
