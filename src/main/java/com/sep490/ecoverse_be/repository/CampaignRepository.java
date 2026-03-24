package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.Campaign;
import com.sep490.ecoverse_be.enums.CampaignType;
import com.sep490.ecoverse_be.enums.PartnershipCampaignStatus;
import com.sep490.ecoverse_be.enums.SchoolCampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    List<Campaign> findByCreatorSchoolIdAndIsActiveTrueOrderByCreatedAtDesc(UUID schoolId);

    List<Campaign> findByCreatorPartnershipIdAndIsActiveTrueOrderByCreatedAtDesc(UUID partnershipId);

    long countByCampaignType(CampaignType campaignType);

    long countBySchoolStatus(SchoolCampaignStatus status);

    long countByPartnershipStatus(PartnershipCampaignStatus status);
}


