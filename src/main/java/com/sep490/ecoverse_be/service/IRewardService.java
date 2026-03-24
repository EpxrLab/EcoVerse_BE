package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.request.CreateRewardRequest;
import com.sep490.ecoverse_be.dto.request.UpdateRewardRequest;
import com.sep490.ecoverse_be.dto.response.RewardResponse;
import com.sep490.ecoverse_be.enums.RewardType;

import java.util.List;
import java.util.UUID;

public interface IRewardService {

    RewardResponse createReward(CreateRewardRequest request);

    List<RewardResponse> getRewardsForSchool(RewardType rewardType);

    List<RewardResponse> getRewardsForStudent(RewardType rewardType);

    List<RewardResponse> getRewardsForParent(RewardType rewardType);

    RewardResponse getRewardById(UUID rewardId);

    RewardResponse updateReward(UUID rewardId, UpdateRewardRequest request);

    void deleteReward(UUID rewardId);

}
