package com.sep490.ecoverse_be.scheduler;

import com.sep490.ecoverse_be.entity.CampaignRewardDelivery;
import com.sep490.ecoverse_be.entity.StudentParentLink;
import com.sep490.ecoverse_be.enums.NotificationType;
import com.sep490.ecoverse_be.enums.PartnershipRewardStatus;
import com.sep490.ecoverse_be.event.NotificationEvent;
import com.sep490.ecoverse_be.repository.CampaignRewardDeliveryRepository;
import com.sep490.ecoverse_be.repository.StudentParentLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PartnershipRewardDeliveryScheduler {

    private final CampaignRewardDeliveryRepository deliveryRepository;
    private final StudentParentLinkRepository studentParentLinkRepository;
    private final ApplicationEventPublisher eventPublisher;

    // Chay moi ngay luc 2:30 SA (lech 30 phut so voi RewardRequestScheduler)
    // Tu dong confirm cac delivery DELIVERED qua 7 ngay ma phu huynh chua xac nhan
//    @Scheduled(cron = "0 30 2 * * *")
    @Scheduled(fixedRate = 60 * 60 * 1000) // 60 phút
    @Transactional
    public void autoConfirmExpiredDeliveries() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(7);

        List<CampaignRewardDelivery> expiredDeliveries = deliveryRepository
                .findByStatusAndDeliveredAtBefore(PartnershipRewardStatus.DELIVERED, deadline);

        if (expiredDeliveries.isEmpty()) {
            return;
        }

        log.info("[PartnershipRewardDeliveryScheduler] Tu dong confirm {} delivery(s) qua han...",
                expiredDeliveries.size());

        for (CampaignRewardDelivery delivery : expiredDeliveries) {
            delivery.setStatus(PartnershipRewardStatus.CONFIRMED);
            delivery.setConfirmedAt(LocalDateTime.now());
            // confirmedBy = null: hệ thống tự động confirm
            deliveryRepository.save(delivery);

            String studentName = delivery.getStudent().getFullName();
            String rewardName = delivery.getCampaignReward().getRewardName();
            String campaignName = delivery.getCampaign().getCampaignName();

            log.info("[PartnershipRewardDeliveryScheduler] Tu dong confirm: student={}, reward={}, campaign={}",
                    studentName, rewardName, campaignName);

            // Thong bao hoc sinh
            eventPublisher.publishEvent(NotificationEvent.builder()
                    .source(this)
                    .recipientUserId(delivery.getStudent().getUser().getId())
                    .type(NotificationType.PARTNERSHIP_REWARD_CONFIRMED)
                    .title("Quà đã được tự động xác nhận")
                    .message("Do không xác nhận trong 7 ngày, phần thưởng \""
                            + rewardName + "\" từ chiến dịch \"" + campaignName
                            + "\" đã được hệ thống tự động xác nhận.")
                    .referenceType("campaign_reward_delivery")
                    .referenceId(delivery.getId())
                    .sendEmail(false)
                    .build());

            // Thong bao phu huynh
            List<StudentParentLink> parentLinks = studentParentLinkRepository
                    .findByStudentId(delivery.getStudent().getId());
            for (StudentParentLink link : parentLinks) {
                eventPublisher.publishEvent(NotificationEvent.builder()
                        .source(this)
                        .recipientUserId(link.getParent().getUser().getId())
                        .type(NotificationType.PARTNERSHIP_REWARD_CONFIRMED)
                        .title("Quà của con bạn hệ thống đã tự động xác nhận")
                        .message("Do không xác nhận trong 7 ngày, phần thưởng \""
                                + rewardName + "\" của " + studentName
                                + " từ chiến dịch \"" + campaignName
                                + "\" đã được hệ thống tự động xác nhận.")
                        .referenceType("campaign_reward_delivery")
                        .referenceId(delivery.getId())
                        .sendEmail(false)
                        .build());
            }

            // Thong bao truong
            eventPublisher.publishEvent(NotificationEvent.builder()
                    .source(this)
                    .recipientUserId(delivery.getSchool().getUser().getId())
                    .type(NotificationType.PARTNERSHIP_REWARD_CONFIRMED)
                    .title("Quà đã được tự động xác nhận")
                    .message("Do phụ huynh không xác nhận trong 7 ngày, phần thưởng \""
                            + rewardName + "\" của học sinh " + studentName
                            + " từ chiến dịch \"" + campaignName
                            + "\" đã được hệ thống tự động xác nhận.")
                    .referenceType("campaign_reward_delivery")
                    .referenceId(delivery.getId())
                    .sendEmail(false)
                    .build());

            // Thong bao partnership
            if (delivery.getCampaign().getCreatorPartnership() != null) {
                eventPublisher.publishEvent(NotificationEvent.builder()
                        .source(this)
                        .recipientUserId(delivery.getCampaign().getCreatorPartnership().getUser().getId())
                        .type(NotificationType.PARTNERSHIP_REWARD_CONFIRMED)
                        .title("Quà đã được tự động xác nhận")
                        .message("Do phụ huynh không xác nhận trong 7 ngày, phần thưởng \""
                                + rewardName + "\" của học sinh " + studentName
                                + " từ chiến dịch \"" + campaignName
                                + "\" đã được hệ thống tự động xác nhận.")
                        .referenceType("campaign_reward_delivery")
                        .referenceId(delivery.getId())
                        .sendEmail(false)
                        .build());
            }
        }

        log.info("[PartnershipRewardDeliveryScheduler] Hoan thanh: {} delivery(s) da duoc tu dong confirm.",
                expiredDeliveries.size());
    }
}
