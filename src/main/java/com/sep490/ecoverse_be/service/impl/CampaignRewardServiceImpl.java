package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.CampaignRewardRequest;
import com.sep490.ecoverse_be.dto.response.CampaignRewardResponse;
import com.sep490.ecoverse_be.entity.Campaign;
import com.sep490.ecoverse_be.entity.CampaignReward;
import com.sep490.ecoverse_be.entity.Partnership;
import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.exception.BadRequestException;
import com.sep490.ecoverse_be.exception.NotFoundException;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.repository.CampaignRepository;
import com.sep490.ecoverse_be.repository.CampaignRewardRepository;
import com.sep490.ecoverse_be.service.ICampaignRewardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CampaignRewardServiceImpl implements ICampaignRewardService {

    @Autowired
    private CampaignRewardRepository campaignRewardRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private S3PresignedUrlService s3PresignedUrlService;

    @Override
    public List<CampaignRewardResponse> getRewards(UUID campaignId) {
        campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy campaign"));
        return campaignRewardRepository.findByCampaignIdOrderByRankPositionAsc(campaignId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void saveRewards(Campaign campaign, Partnership partnership, List<CampaignRewardRequest> rewards) {
        if (rewards == null || rewards.isEmpty()) {
            return;
        }

        int topRankingCount = campaign.getTopRankingCount() != null ? campaign.getTopRankingCount() : 10;

        Set<Integer> seenRanks = new HashSet<>();
        for (CampaignRewardRequest req : rewards) {
            if (req.getRankPosition() < 1 || req.getRankPosition() > topRankingCount) {
                throw new BadRequestException(
                        "rankPosition phải trong khoảng [1, " + topRankingCount + "]. Giá trị không hợp lệ: " + req.getRankPosition());
            }
            if (!seenRanks.add(req.getRankPosition())) {
                throw new BadRequestException("rankPosition bị trùng trong danh sách: " + req.getRankPosition());
            }
        }

        campaignRewardRepository.deleteByCampaignId(campaign.getId());
        campaignRewardRepository.flush();

        User currentUser = getCurrentUser();
        for (CampaignRewardRequest req : rewards) {
            CampaignReward reward = new CampaignReward();
            reward.setCampaign(campaign);
            reward.setPartnership(partnership);
            reward.setRankPosition(req.getRankPosition());
            reward.setRewardName(req.getRewardName());
            reward.setDescription(req.getDescription());
            reward.setImageUrl(req.getImageUrl());
            reward.setSponsorName(req.getSponsorName());
            reward.setCreatedBy(currentUser);
            campaignRewardRepository.save(reward);
        }
    }

    private CampaignRewardResponse mapToResponse(CampaignReward r) {
        return CampaignRewardResponse.builder()
                .id(r.getId())
                .campaignId(r.getCampaign().getId())
                .rankPosition(r.getRankPosition())
                .rewardName(r.getRewardName())
                .description(r.getDescription())
                .imageUrl(r.getImageUrl())
                .imagePresignedUrl(s3PresignedUrlService.generatePresignedUrl(r.getImageUrl()))
                .sponsorName(r.getSponsorName())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BadRequestException("Không xác định được người dùng hiện tại");
        }
        return principal.getUser();
    }
}
