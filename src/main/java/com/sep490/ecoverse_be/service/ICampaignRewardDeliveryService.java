package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.request.DeliverRewardDeliveryRequest;
import com.sep490.ecoverse_be.dto.request.ShipRewardDeliveryRequest;
import com.sep490.ecoverse_be.dto.response.CampaignRewardDeliveryResponse;
import com.sep490.ecoverse_be.enums.PartnershipRewardStatus;

import java.util.List;
import java.util.UUID;

public interface ICampaignRewardDeliveryService {

    // Partnership: xem tat ca deliveries cua campaign (co loc theo status)
    List<CampaignRewardDeliveryResponse> getDeliveriesByCampaign(UUID campaignId, PartnershipRewardStatus status);

    // School: xem deliveries cua truong minh trong campaign
    List<CampaignRewardDeliveryResponse> getDeliveriesBySchool(UUID campaignId, PartnershipRewardStatus status);

    // Student/Parent: xem qua cua minh / con
    List<CampaignRewardDeliveryResponse> getMyDeliveries();

    // Partnership: chuyen PREPARING -> SHIPPING
    CampaignRewardDeliveryResponse shipReward(UUID deliveryId, ShipRewardDeliveryRequest request);

    // School: chuyen SHIPPING -> ARRIVED
    CampaignRewardDeliveryResponse confirmArrived(UUID deliveryId);

    // School: chuyen ARRIVED -> DELIVERED
    CampaignRewardDeliveryResponse markDelivered(UUID deliveryId, DeliverRewardDeliveryRequest request);

    // Parent: chuyen DELIVERED -> CONFIRMED
    CampaignRewardDeliveryResponse confirmReceived(UUID deliveryId);
}
