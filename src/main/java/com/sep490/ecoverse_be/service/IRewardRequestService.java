package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.request.CancelRewardRequestDto;
import com.sep490.ecoverse_be.dto.request.CreateRewardRequestDto;
import com.sep490.ecoverse_be.dto.request.RejectRewardRequestDto;
import com.sep490.ecoverse_be.dto.response.RewardRequestResponse;
import com.sep490.ecoverse_be.dto.response.RewardRequestTrackingResponse;
import com.sep490.ecoverse_be.enums.RewardRequestStatus;

import java.util.List;
import java.util.UUID;

public interface IRewardRequestService {

    RewardRequestResponse createRequest(CreateRewardRequestDto request);

    List<RewardRequestResponse> getMyRequests();

    RewardRequestResponse cancelRequest(UUID requestId, CancelRewardRequestDto dto);

    List<RewardRequestResponse> getSchoolRequests(RewardRequestStatus status);

    RewardRequestResponse approveOrRejectRequest(UUID requestId, RejectRewardRequestDto dto);

    RewardRequestResponse markDelivered(UUID requestId, String imageUrl);

    RewardRequestResponse confirmReceived(UUID requestId);

    RewardRequestTrackingResponse getRequestTracking(UUID requestId);

    UUID getCurrentSchoolId();
}
