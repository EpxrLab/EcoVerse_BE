package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.DeliverRewardDeliveryRequest;
import com.sep490.ecoverse_be.dto.request.ShipRewardDeliveryRequest;
import com.sep490.ecoverse_be.dto.response.CampaignRewardDeliveryResponse;
import com.sep490.ecoverse_be.entity.*;
import com.sep490.ecoverse_be.enums.NotificationType;
import com.sep490.ecoverse_be.enums.PartnershipRewardStatus;
import com.sep490.ecoverse_be.enums.Role;
import com.sep490.ecoverse_be.event.NotificationEvent;
import com.sep490.ecoverse_be.exception.BadRequestException;
import com.sep490.ecoverse_be.exception.NotFoundException;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.repository.*;
import com.sep490.ecoverse_be.service.ICampaignRewardDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CampaignRewardDeliveryServiceImpl implements ICampaignRewardDeliveryService {

    private final CampaignRewardDeliveryRepository deliveryRepository;
    private final SchoolRepository schoolRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final PartnershipRepository partnershipRepository;
    private final StudentParentLinkRepository studentParentLinkRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final S3PresignedUrlService s3PresignedUrlService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ((UserPrincipal) auth.getPrincipal()).getUser();
    }

    private CampaignRewardDeliveryResponse mapToResponse(CampaignRewardDelivery d) {
        CampaignReward reward = d.getCampaignReward();
        Student student = d.getStudent();
        School school = d.getSchool();
        Campaign campaign = d.getCampaign();

        String rewardImagePresignedUrl = reward.getImageUrl() != null
                ? s3PresignedUrlService.generatePresignedUrl(reward.getImageUrl()) : null;

        String deliveryImagePresignedUrl = d.getDeliveryImageUrl() != null
                ? s3PresignedUrlService.generatePresignedUrl(d.getDeliveryImageUrl()) : null;

        String arrivedConfirmedByName = d.getArrivedConfirmedBy() != null
                ? d.getArrivedConfirmedBy().getEmail() : null;
        String deliveredByName = d.getDeliveredBy() != null
                ? d.getDeliveredBy().getEmail() : null;
        String confirmedByName = d.getConfirmedBy() != null
                ? d.getConfirmedBy().getUser().getEmail() : null;

        return CampaignRewardDeliveryResponse.builder()
                .id(d.getId())
                .campaignId(campaign.getId())
                .campaignName(campaign.getCampaignName())
                .campaignRewardId(reward.getId())
                .rewardName(reward.getRewardName())
                .rewardImageUrl(reward.getImageUrl())
                .rewardImagePresignedUrl(rewardImagePresignedUrl)
                .rankPosition(reward.getRankPosition())
                .studentId(student.getId())
                .studentName(student.getFullName())
                .studentCode(student.getStudentCode())
                .schoolId(school.getId())
                .schoolName(school.getSchoolName())
                .schoolAddress(school.getAddress())
                .schoolWard(school.getWard())
                .schoolProvince(school.getProvince())
                .leaderboardRank(d.getLeaderboardRank())
                .totalScore(d.getRoundLeaderboard() != null
                        ? d.getRoundLeaderboard().getCombinedAccuracyPercentage() : null)
                .status(d.getStatus())
                .preparingAt(d.getPreparingAt())
                .shippedAt(d.getShippedAt())
                .arrivedAt(d.getArrivedAt())
                .deliveredAt(d.getDeliveredAt())
                .confirmedAt(d.getConfirmedAt())
                .shippingTrackingCode(d.getShippingTrackingCode())
                .deliveryImageUrl(d.getDeliveryImageUrl())
                .deliveryImagePresignedUrl(deliveryImagePresignedUrl)
                .arrivedConfirmedByName(arrivedConfirmedByName)
                .deliveredByName(deliveredByName)
                .confirmedByName(confirmedByName)
                .notes(d.getNotes())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    @Override
    public List<CampaignRewardDeliveryResponse> getDeliveriesByCampaign(UUID campaignId, PartnershipRewardStatus status) {
        User currentUser = getCurrentUser();
        Partnership partnership = partnershipRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin partnership"));

        List<CampaignRewardDelivery> deliveries = status != null
                ? deliveryRepository.findByCampaignIdAndStatusOrderByLeaderboardRankAsc(campaignId, status)
                : deliveryRepository.findByCampaignIdOrderByLeaderboardRankAsc(campaignId);

        // Chi lay deliveries cua campaign thuoc partnership nay
        return deliveries.stream()
                .filter(d -> {
                    Partnership campaignPartnership = d.getCampaign().getCreatorPartnership();
                    return campaignPartnership != null && campaignPartnership.getId().equals(partnership.getId());
                })
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CampaignRewardDeliveryResponse> getDeliveriesBySchool(UUID campaignId, PartnershipRewardStatus status) {
        User currentUser = getCurrentUser();
        School school = schoolRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin trường"));

        List<CampaignRewardDelivery> deliveries = status != null
                ? deliveryRepository.findBySchoolIdAndCampaignIdAndStatusOrderByLeaderboardRankAsc(school.getId(), campaignId, status)
                : deliveryRepository.findBySchoolIdAndCampaignIdOrderByLeaderboardRankAsc(school.getId(), campaignId);

        return deliveries.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CampaignRewardDeliveryResponse> getMyDeliveries() {
        User currentUser = getCurrentUser();

        List<CampaignRewardDelivery> deliveries;
        if (currentUser.getRole() == Role.STUDENT) {
            Student student = studentRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy học sinh"));
            deliveries = deliveryRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());
        } else {
            Parent parent = parentRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy phụ huynh"));
            deliveries = studentParentLinkRepository.findByParentId(parent.getId()).stream()
                    .flatMap(link -> deliveryRepository
                            .findByStudentIdOrderByCreatedAtDesc(link.getStudent().getId()).stream())
                    .collect(Collectors.toList());
        }

        return deliveries.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CampaignRewardDeliveryResponse shipReward(UUID deliveryId, ShipRewardDeliveryRequest request) {
        User currentUser = getCurrentUser();
        Partnership partnership = partnershipRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin partnership"));

        CampaignRewardDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy delivery record"));

        // Kiem tra delivery thuoc ve campaign cua partnership nay
        Partnership campaignPartnership = delivery.getCampaign().getCreatorPartnership();
        if (campaignPartnership == null || !campaignPartnership.getId().equals(partnership.getId())) {
            throw new BadRequestException("Bạn không có quyền thao tác trên delivery này");
        }

        if (delivery.getStatus() != PartnershipRewardStatus.PREPARING) {
            throw new BadRequestException(
                    "Chỉ có thể ship khi status là PREPARING. Status hiện tại: " + delivery.getStatus());
        }

        delivery.setStatus(PartnershipRewardStatus.SHIPPING);
        delivery.setShippedAt(OffsetDateTime.now());
        if (request.getTrackingCode() != null && !request.getTrackingCode().isBlank()) {
            delivery.setShippingTrackingCode(request.getTrackingCode());
        }
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            delivery.setNotes(request.getNotes());
        }
        deliveryRepository.save(delivery);

        String studentName = delivery.getStudent().getFullName();
        String rewardName = delivery.getCampaignReward().getRewardName();
        String campaignName = delivery.getCampaign().getCampaignName();
        String trackingInfo = request.getTrackingCode() != null && !request.getTrackingCode().isBlank()
                ? " Mã vận đơn: " + request.getTrackingCode() : "";

        // Thong bao hoc sinh
        publishEvent(
                delivery.getStudent().getUser().getId(),
                NotificationType.PARTNERSHIP_REWARD_SHIPPING,
                "Quà của bạn đang được vận chuyển!",
                "Phần thưởng \"" + rewardName + "\" từ chiến dịch \"" + campaignName
                        + "\" đang được vận chuyển về trường của bạn." + trackingInfo,
                delivery.getId()
        );

        // Thông báo phụ huynh
        publishEventToParents(
                delivery.getStudent().getId(),
                NotificationType.PARTNERSHIP_REWARD_SHIPPING,
                "Quà của con đang được vận chuyển!",
                "Phần thưởng \"" + rewardName + "\" của " + studentName
                        + " từ chiến dịch \"" + campaignName + "\" đang được vận chuyển về trường." + trackingInfo,
                delivery.getId()
        );

        // Thông báo trường
        publishEvent(
                delivery.getSchool().getUser().getId(),
                NotificationType.PARTNERSHIP_REWARD_SHIPPING,
                "Quà đang được vận chuyển đến trường",
                "Quà \"" + rewardName + "\" dành cho học sinh " + studentName
                        + " của chiến dịch \"" + campaignName + "\" đang được vận chuyển." + trackingInfo,
                delivery.getId()
        );

        return mapToResponse(delivery);
    }

    @Override
    @Transactional
    public CampaignRewardDeliveryResponse confirmArrived(UUID deliveryId) {
        User currentUser = getCurrentUser();
        School school = schoolRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin trường"));

        CampaignRewardDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy delivery record"));

        if (!delivery.getSchool().getId().equals(school.getId())) {
            throw new BadRequestException("Bạn không có quyền thao tác trên delivery này");
        }

        if (delivery.getStatus() != PartnershipRewardStatus.SHIPPING) {
            throw new BadRequestException(
                    "Chỉ có thể xác nhận khi status là SHIPPING. Status hiện tại: " + delivery.getStatus());
        }

        delivery.setStatus(PartnershipRewardStatus.ARRIVED);
        delivery.setArrivedAt(OffsetDateTime.now());
        delivery.setArrivedConfirmedBy(currentUser);
        deliveryRepository.save(delivery);

        String studentName = delivery.getStudent().getFullName();
        String rewardName = delivery.getCampaignReward().getRewardName();
        String campaignName = delivery.getCampaign().getCampaignName();
        String schoolName = delivery.getSchool().getSchoolName();

        // Thong bao hoc sinh
        publishEvent(
                delivery.getStudent().getUser().getId(),
                NotificationType.PARTNERSHIP_REWARD_ARRIVED,
                "Quà của bạn đã đến trường!",
                "Phần thưởng \"" + rewardName + "\" từ chiến dịch \"" + campaignName
                        + "\" đã đến " + schoolName + ". Nhà trường sẽ giao cho bạn sớm.",
                delivery.getId()
        );

        // Thong bao phu huynh
        publishEventToParents(
                delivery.getStudent().getId(),
                NotificationType.PARTNERSHIP_REWARD_ARRIVED,
                "Quà của con đã đến trường!",
                "Phần thưởng \"" + rewardName + "\" của " + studentName
                        + " từ chiến dịch \"" + campaignName + "\" đã đến " + schoolName + ".",
                delivery.getId()
        );

        // Thong bao partnership
        publishEventToPartnership(
                delivery.getCampaign(),
                NotificationType.PARTNERSHIP_REWARD_ARRIVED,
                "Trường đã xác nhận nhận quà",
                "Trường " + schoolName + " đã xác nhận nhận quà \"" + rewardName
                        + "\" cho học sinh " + studentName + " từ chiến dịch \"" + campaignName + "\".",
                delivery.getId()
        );

        return mapToResponse(delivery);
    }

    @Override
    @Transactional
    public CampaignRewardDeliveryResponse markDelivered(UUID deliveryId, DeliverRewardDeliveryRequest request) {
        User currentUser = getCurrentUser();
        School school = schoolRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin school"));

        CampaignRewardDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy delivery record"));

        if (!delivery.getSchool().getId().equals(school.getId())) {
            throw new BadRequestException("Bạn không có quyền thao tác trên delivery này");
        }

        if (delivery.getStatus() != PartnershipRewardStatus.ARRIVED) {
            throw new BadRequestException(
                    "Chỉ có thể giao quà khi status là ARRIVED. Status hiện tại: " + delivery.getStatus());
        }

        delivery.setStatus(PartnershipRewardStatus.DELIVERED);
        delivery.setDeliveredAt(OffsetDateTime.now());
        delivery.setDeliveredBy(currentUser);
        delivery.setDeliveryImageUrl(request.getDeliveryImageUrl());
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            delivery.setNotes(request.getNotes());
        }
        deliveryRepository.save(delivery);

        String studentName = delivery.getStudent().getFullName();
        String rewardName = delivery.getCampaignReward().getRewardName();
        String campaignName = delivery.getCampaign().getCampaignName();
        String schoolName = delivery.getSchool().getSchoolName();

        // Thong bao hoc sinh
        publishEvent(
                delivery.getStudent().getUser().getId(),
                NotificationType.PARTNERSHIP_REWARD_DELIVERED,
                "Quà của bạn đã được giao!",
                "Phần thưởng \"" + rewardName + "\" từ chiến dịch \"" + campaignName
                        + "\" đã được " + schoolName + " giao đến bạn.",
                delivery.getId()
        );

        publishEventToParents(
                delivery.getStudent().getId(),
                NotificationType.PARTNERSHIP_REWARD_DELIVERED,
                "Quà của con đã được giao! Cần xác nhận.",
                "Phần thưởng \"" + rewardName + "\" của " + studentName
                        + " từ chiến dịch \"" + campaignName + "\" đã được giao. Vui lòng xác nhận đã nhận quà.",
                delivery.getId()
        );

        // Thong bao partnership
        publishEventToPartnership(
                delivery.getCampaign(),
                NotificationType.PARTNERSHIP_REWARD_DELIVERED,
                "Quà đã được giao đến học sinh",
                "Quà \"" + rewardName + "\" đã được giao cho học sinh " + studentName
                        + " từ chiến dịch \"" + campaignName + "\".",
                delivery.getId()
        );

        return mapToResponse(delivery);
    }

    @Override
    @Transactional
    public CampaignRewardDeliveryResponse confirmReceived(UUID deliveryId) {
        User currentUser = getCurrentUser();
        Parent parent = parentRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin phụ huynh"));

        CampaignRewardDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy delivery record"));

        // Kiem tra phu huynh co lien ket voi hoc sinh trong delivery
        boolean isLinked = studentParentLinkRepository
                .existsByStudentIdAndParentId(delivery.getStudent().getId(), parent.getId());
        if (!isLinked) {
            throw new BadRequestException("Bạn không có quyền xác nhận học sinh này");
        }

        if (delivery.getStatus() != PartnershipRewardStatus.DELIVERED) {
            throw new BadRequestException(
                    "Chỉ có thể xác nhận khi status là DELIVERED. Status hiện tại: " + delivery.getStatus());
        }

        delivery.setStatus(PartnershipRewardStatus.CONFIRMED);
        delivery.setConfirmedAt(OffsetDateTime.now());
        delivery.setConfirmedBy(parent);
        deliveryRepository.save(delivery);

        String studentName = delivery.getStudent().getFullName();
        String rewardName = delivery.getCampaignReward().getRewardName();
        String campaignName = delivery.getCampaign().getCampaignName();
        String schoolName = delivery.getSchool().getSchoolName();

        // Thong bao hoc sinh
        publishEvent(
                delivery.getStudent().getUser().getId(),
                NotificationType.PARTNERSHIP_REWARD_CONFIRMED,
                "Phụ huynh đã xác nhận nhận quà!",
                "Phần thưởng \"" + rewardName + "\" từ chiến dịch \"" + campaignName
                        + "\" đã được phụ huynh xác nhận nhận thành công.",
                delivery.getId()
        );

        // Thong bao truong
        publishEvent(
                delivery.getSchool().getUser().getId(),
                NotificationType.PARTNERSHIP_REWARD_CONFIRMED,
                "Phụ huynh đã xác nhận nhận quà",
                "Phụ huynh của học sinh " + studentName + " đã xác nhận nhận phần thưởng \""
                        + rewardName + "\" từ chiến dịch \"" + campaignName + "\".",
                delivery.getId()
        );

        // Thong bao partnership
        publishEventToPartnership(
                delivery.getCampaign(),
                NotificationType.PARTNERSHIP_REWARD_CONFIRMED,
                "Phụ huynh đã xác nhận nhận quà",
                "Phụ huynh của học sinh " + studentName + " (trường " + schoolName
                        + ") đã xác nhận nhận phần thưởng \"" + rewardName
                        + "\" từ chiến dịch \"" + campaignName + "\".",
                delivery.getId()
        );

        return mapToResponse(delivery);
    }

    // -- Helper methods --

    private void publishEvent(UUID recipientUserId, NotificationType type,
                              String title, String message, UUID deliveryId) {
        eventPublisher.publishEvent(NotificationEvent.builder()
                .source(this)
                .recipientUserId(recipientUserId)
                .type(type)
                .title(title)
                .message(message)
                .referenceType("campaign_reward_delivery")
                .referenceId(deliveryId)
                .sendEmail(false)
                .build());
    }

    private void publishEventToParents(UUID studentId, NotificationType type,
                                       String title, String message, UUID deliveryId) {
        List<StudentParentLink> parentLinks = studentParentLinkRepository.findByStudentId(studentId);
        for (StudentParentLink link : parentLinks) {
            publishEvent(link.getParent().getUser().getId(), type, title, message, deliveryId);
        }
    }

    private void publishEventToPartnership(Campaign campaign, NotificationType type,
                                           String title, String message, UUID deliveryId) {
        Partnership partnership = campaign.getCreatorPartnership();
        if (partnership != null) {
            publishEvent(partnership.getUser().getId(), type, title, message, deliveryId);
        }
    }
}
