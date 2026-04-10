package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.request.CampaignRewardRequest;
import com.sep490.ecoverse_be.dto.response.CampaignRewardResponse;
import com.sep490.ecoverse_be.entity.Campaign;
import com.sep490.ecoverse_be.entity.Partnership;

import java.util.List;
import java.util.UUID;

public interface ICampaignRewardService {

    List<CampaignRewardResponse> getRewards(UUID campaignId);

    void saveRewards(Campaign campaign, Partnership partnership, List<CampaignRewardRequest> rewards);
}
