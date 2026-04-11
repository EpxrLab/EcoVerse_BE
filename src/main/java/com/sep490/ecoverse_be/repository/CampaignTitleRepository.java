package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.CampaignTitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CampaignTitleRepository extends JpaRepository<CampaignTitle, UUID> {

    List<CampaignTitle> findByCampaignIdAndIsActiveTrue(UUID campaignId);

    List<CampaignTitle> findByCampaignId(UUID campaignId);
}
