package com.sep490.ecoverse_be.scheduler;

import com.sep490.ecoverse_be.entity.*;
import com.sep490.ecoverse_be.enums.NotificationType;
import com.sep490.ecoverse_be.enums.ParticipationStatus;
import com.sep490.ecoverse_be.enums.PartnershipCampaignStatus;
import com.sep490.ecoverse_be.enums.PartnershipRewardStatus;
import com.sep490.ecoverse_be.enums.RoundStatus;
import com.sep490.ecoverse_be.enums.SchoolCampaignStatus;
import com.sep490.ecoverse_be.event.NotificationEvent;
import com.sep490.ecoverse_be.repository.*;
import com.sep490.ecoverse_be.service.INotificationService;
import com.sep490.ecoverse_be.service.impl.TitleEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CampaignScheduler {

    private final CampaignRepository campaignRepository;
    private final CampaignParticipantRepository campaignParticipantRepository;
    private final CampaignSchoolParticipateRepository campaignSchoolParticipateRepository;
    private final StudentParentLinkRepository studentParentLinkRepository;
    private final INotificationService notificationService;
    private final CampaignRoundRepository campaignRoundRepository;
    private final RoundLeaderboardRepository roundLeaderboardRepository;
    private final CampaignRewardRepository campaignRewardRepository;
    private final CampaignRewardDeliveryRepository campaignRewardDeliveryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TitleEvaluationService titleEvaluationService;

    /**
     * Chạy mỗi phút để tự động chuyển trạng thái campaign trường (SCHOOL_INTERNAL).
     * Flow: SCHEDULED → INVITING → ON_GOING → COMPLETED
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void autoTransitionSchoolCampaignStatuses() {
        OffsetDateTime now = OffsetDateTime.now();

        transitionToInviting(now);
        transitionToOnGoing(now);
        transitionToCompleted(now);
    }

    /**
     * Partnership: SCHEDULED → JOINING → INVITING → ON_GOING → COMPLETED
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void autoTransitionPartnershipCampaignStatuses() {
        OffsetDateTime now = OffsetDateTime.now();
        partnershipToJoining(now);
        partnershipToInviting(now);
        partnershipToOnGoing(now);
        partnershipToCompleted(now);
    }

    // SCHEDULED → INVITING: khi invitationDate đã đến
    private void transitionToInviting(OffsetDateTime now) {
        List<Campaign> campaigns = campaignRepository.findSchoolCampaignsReadyForInviting(now);
        if (campaigns.isEmpty()) return;

        for (Campaign campaign : campaigns) {
            campaign.setSchoolStatus(SchoolCampaignStatus.INVITING);
            campaignRepository.save(campaign);

            List<CampaignParticipant> pending = campaignParticipantRepository
                    .findByCampaignIdAndInvitationSentAtIsNullAndIsActiveTrue(campaign.getId());
            for (CampaignParticipant p : pending) {
                if (p.getParentApprovalStatus() == ParticipationStatus.PREPARED) {
                    p.setParentApprovalStatus(ParticipationStatus.INVITED);
                }
                p.setInvitationSentAt(now);
                campaignParticipantRepository.save(p);
            }

            notificationService.notifyCampaignParents(
                    campaign.getId(),
                    NotificationType.CAMPAIGN_INVITE,
                    "Lời mời tham gia chiến dịch",
                    "Con bạn được mời tham gia chiến dịch \"" + campaign.getCampaignName() + "\". Vui lòng xét duyệt trong thời hạn quy định.",
                    "campaign",
                    campaign.getId(),
                    null
            );

            log.info("[CampaignScheduler] Campaign '{}' ({}): SCHEDULED → INVITING",
                    campaign.getCampaignName(), campaign.getCampaignCode());
        }
        log.info("[CampaignScheduler] {} campaign(s) transitioned to INVITING", campaigns.size());
    }

    // INVITING / EXTENDED → ON_GOING: khi startDate đã đến
    private void transitionToOnGoing(OffsetDateTime now) {
        List<Campaign> campaigns = campaignRepository.findSchoolCampaignsReadyForOnGoing(now);
        if (campaigns.isEmpty()) return;

        for (Campaign campaign : campaigns) {
            // Truoc khi chuyen ON_GOING: tu dong tu choi tat ca PREPARED (cho phu huynh duyet) con lai
            autoRejectPendingParentApprovals(campaign);

            campaign.setSchoolStatus(SchoolCampaignStatus.ON_GOING);
            campaignRepository.save(campaign);
            log.info("[CampaignScheduler] Campaign '{}' ({}): → ON_GOING",
                    campaign.getCampaignName(), campaign.getCampaignCode());

            // Tự động reject các invitation chưa được APPROVED
            autoRejectPendingInvitations(campaign);

            // Broadcast toi tat ca hoc sinh tham gia
            notificationService.notifyCampaignParticipants(
                    campaign.getId(),
                    NotificationType.CAMPAIGN_START,
                    "Chiến dịch đã bắt đầu!",
                    "Chiến dịch \"" + campaign.getCampaignName() + "\" đã bắt đầu. Hãy vào thi đấu ngay!",
                    "campaign",
                    campaign.getId(),
                    null
            );

            notificationService.notifyCampaignParents(
                    campaign.getId(),
                    NotificationType.CAMPAIGN_START,
                    "Chiến dịch của con đã bắt đầu",
                    "Chiến dịch \"" + campaign.getCampaignName() + "\" mà con bạn tham gia đã chính thức bắt đầu.",
                    "campaign",
                    campaign.getId(),
                    null
            );
        }
        log.info("[CampaignScheduler] {} campaign(s) transitioned to ON_GOING", campaigns.size());
    }

    // ON_GOING → COMPLETED: khi endDate đã đến
    private void transitionToCompleted(OffsetDateTime now) {
        List<Campaign> campaigns = campaignRepository.findSchoolCampaignsReadyForCompleted(now);
        if (campaigns.isEmpty()) return;

        for (Campaign campaign : campaigns) {
            campaign.setSchoolStatus(SchoolCampaignStatus.COMPLETED);
            campaignRepository.save(campaign);
            log.info("[CampaignScheduler] Campaign '{}' ({}): ON_GOING → COMPLETED",
                    campaign.getCampaignName(), campaign.getCampaignCode());

            // Trao danh hiệu cho học sinh thỏa mãn tiêu chí
            titleEvaluationService.evaluateTitles(campaign);

            notificationService.notifyCampaignParticipants(
                    campaign.getId(),
                    NotificationType.CAMPAIGN_END,
                    "Chiến dịch đã kết thúc!",
                    "Chiến dịch \"" + campaign.getCampaignName() + "\" đã kết thúc. Kiểm tra kết quả và xu thưởng của bạn ngay!",
                    "campaign",
                    campaign.getId(),
                    null
            );

            notificationService.notifyCampaignParents(
                    campaign.getId(),
                    NotificationType.CAMPAIGN_END,
                    "Chiến dịch của con đã kết thúc",
                    "Chiến dịch \"" + campaign.getCampaignName() + "\" mà con bạn tham gia đã kết thúc. Hãy kiểm tra kết quả của con.",
                    "campaign",
                    campaign.getId(),
                    null
            );
        }
        log.info("[CampaignScheduler] {} campaign(s) transitioned to COMPLETED", campaigns.size());
    }

    private void partnershipToJoining(OffsetDateTime now) {
        List<Campaign> campaigns = campaignRepository.findPartnershipCampaignsReadyForJoining(now);
        if (campaigns.isEmpty()) return;

        for (Campaign campaign : campaigns) {
            campaign.setPartnershipStatus(PartnershipCampaignStatus.JOINING);
            campaignRepository.save(campaign);

            List<CampaignSchoolParticipate> pendingSchools =
                    campaignSchoolParticipateRepository.findByCampaignIdAndInvitationSentAtIsNull(campaign.getId());
            for (CampaignSchoolParticipate sp : pendingSchools) {
                sp.setStatus(ParticipationStatus.INVITED);
                sp.setInvitationSentAt(now);
                campaignSchoolParticipateRepository.save(sp);
            }

            List<User> schoolUsers = pendingSchools.stream()
                    .map(sp -> sp.getSchool().getUser())
                    .distinct()
                    .toList();
            if (!schoolUsers.isEmpty()) {
                notificationService.notifyUsers(
                        schoolUsers,
                        NotificationType.CAMPAIGN_INVITE,
                        "Lời mời tham gia chiến dịch liên kết",
                        "Trường của bạn được mời tham gia chiến dịch \"" + campaign.getCampaignName() + "\". Vui lòng xem chi tiết và phản hồi.",
                        "campaign",
                        campaign.getId(),
                        null,
                        false
                );
            }

            log.info("[CampaignScheduler] Partnership campaign '{}' ({}): SCHEDULED → JOINING",
                    campaign.getCampaignName(), campaign.getCampaignCode());
        }
        log.info("[CampaignScheduler] {} partnership campaign(s) transitioned to JOINING", campaigns.size());
    }

    private void partnershipToInviting(OffsetDateTime now) {
        List<Campaign> campaigns = campaignRepository.findPartnershipCampaignsReadyForInviting(now);
        if (campaigns.isEmpty()) return;

        for (Campaign campaign : campaigns) {
            // Truoc khi chuyen INVITING: tu dong tu choi tat ca truong con INVITED (chua phan hoi)
            autoRejectPendingSchoolInvitations(campaign);

            campaign.setPartnershipStatus(PartnershipCampaignStatus.INVITING);
            campaignRepository.save(campaign);

            List<CampaignParticipant> pending = campaignParticipantRepository
                    .findByCampaignIdAndInvitationSentAtIsNullAndIsActiveTrue(campaign.getId());
            for (CampaignParticipant p : pending) {
                if (p.getParentApprovalStatus() == ParticipationStatus.PREPARED) {
                    p.setParentApprovalStatus(ParticipationStatus.INVITED);
                }
                p.setInvitationSentAt(now);
                campaignParticipantRepository.save(p);
            }

            notificationService.notifyCampaignParents(
                    campaign.getId(),
                    NotificationType.CAMPAIGN_INVITE,
                    "Lời mời tham gia chiến dịch",
                    "Con bạn được mời tham gia chiến dịch \"" + campaign.getCampaignName() + "\". Vui lòng xét duyệt trong thời hạn quy định.",
                    "campaign",
                    campaign.getId(),
                    null
            );

            log.info("[CampaignScheduler] Partnership campaign '{}' ({}): JOINING → INVITING",
                    campaign.getCampaignName(), campaign.getCampaignCode());
        }
        log.info("[CampaignScheduler] {} partnership campaign(s) transitioned to INVITING", campaigns.size());
    }

    private void partnershipToOnGoing(OffsetDateTime now) {
        List<Campaign> campaigns = campaignRepository.findPartnershipCampaignsReadyForOnGoing(now);
        if (campaigns.isEmpty()) return;

        for (Campaign campaign : campaigns) {
            // Truoc khi chuyen ON_GOING: tu dong tu choi tat ca PREPARED (cho phu huynh duyet) con lai
            autoRejectPendingParentApprovals(campaign);

            campaign.setPartnershipStatus(PartnershipCampaignStatus.ON_GOING);
            campaignRepository.save(campaign);

            // Tu dong reject cac hoc sinh chua APPROVED (giong school campaign)
            autoRejectPendingInvitations(campaign);

            notificationService.notifyCampaignParticipants(
                    campaign.getId(),
                    NotificationType.CAMPAIGN_START,
                    "Chiến dịch đã bắt đầu!",
                    "Chiến dịch \"" + campaign.getCampaignName() + "\" đã bắt đầu. Hãy vào thi đấu ngay!",
                    "campaign",
                    campaign.getId(),
                    null
            );

            notificationService.notifyCampaignParents(
                    campaign.getId(),
                    NotificationType.CAMPAIGN_START,
                    "Chiến dịch của con đã bắt đầu",
                    "Chiến dịch \"" + campaign.getCampaignName() + "\" mà con bạn tham gia đã chính thức bắt đầu.",
                    "campaign",
                    campaign.getId(),
                    null
            );

            log.info("[CampaignScheduler] Partnership campaign '{}' ({}): INVITING → ON_GOING",
                    campaign.getCampaignName(), campaign.getCampaignCode());
        }
        log.info("[CampaignScheduler] {} partnership campaign(s) transitioned to ON_GOING", campaigns.size());
    }

    private void partnershipToCompleted(OffsetDateTime now) {
        List<Campaign> campaigns = campaignRepository.findPartnershipCampaignsReadyForCompleted(now);
        if (campaigns.isEmpty()) return;

        for (Campaign campaign : campaigns) {
            campaign.setPartnershipStatus(PartnershipCampaignStatus.COMPLETED);
            campaignRepository.save(campaign);

            // Trao danh hiệu cho học sinh thỏa mãn tiêu chí
            titleEvaluationService.evaluateTitles(campaign);

            // Tao delivery records cho hoc sinh dat giai cua vong cuoi
            createDeliveryRecordsForWinners(campaign);

            notificationService.notifyCampaignParticipants(
                    campaign.getId(),
                    NotificationType.CAMPAIGN_END,
                    "Chiến dịch đã kết thúc!",
                    "Chiến dịch \"" + campaign.getCampaignName() + "\" đã kết thúc. Kiểm tra kết quả và xu thưởng của bạn ngay!",
                    "campaign",
                    campaign.getId(),
                    null
            );

            notificationService.notifyCampaignParents(
                    campaign.getId(),
                    NotificationType.CAMPAIGN_END,
                    "Chiến dịch của con đã kết thúc",
                    "Chiến dịch \"" + campaign.getCampaignName() + "\" mà con bạn tham gia đã kết thúc. Hãy kiểm tra kết quả của con.",
                    "campaign",
                    campaign.getId(),
                    null
            );

            log.info("[CampaignScheduler] Partnership campaign '{}' ({}): ON_GOING → COMPLETED",
                    campaign.getCampaignName(), campaign.getCampaignCode());
        }
        log.info("[CampaignScheduler] {} partnership campaign(s) transitioned to COMPLETED", campaigns.size());
    }

    private void createDeliveryRecordsForWinners(Campaign campaign) {
        // Tim vong cuoi (isFinalRound = true) va phai o trang thai COMPLETED
        Optional<CampaignRound> finalRoundOpt = campaignRoundRepository
                .findByCampaignIdOrderByRoundNumberAsc(campaign.getId())
                .stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsFinalRound()) && r.getStatus() == RoundStatus.COMPLETED)
                .findFirst();

        if (finalRoundOpt.isEmpty()) {
            log.warn("[CampaignScheduler] Khong tim thay final round COMPLETED cho campaign '{}'",
                    campaign.getCampaignName());
            return;
        }

        CampaignRound finalRound = finalRoundOpt.get();

        List<RoundLeaderboard> ranked = roundLeaderboardRepository
                .findByCampaignRoundIdOrderByCombinedAccuracyPercentageDescAvgTimeSecondsAsc(finalRound.getId());
        if (ranked.isEmpty()) {
            log.warn("[CampaignScheduler] Khong co du lieu leaderboard cho final round cua campaign '{}'",
                    campaign.getCampaignName());
            return;
        }

        int topRankingCount = campaign.getTopRankingCount() == null ? 0 : campaign.getTopRankingCount();
        if (topRankingCount <= 0) return;

        List<RoundLeaderboard> winners = ranked.stream().limit(topRankingCount).toList();

        Map<Integer, CampaignReward> rewardByRank = campaignRewardRepository
                .findByCampaignIdOrderByRankPositionAsc(campaign.getId())
                .stream()
                .collect(Collectors.toMap(CampaignReward::getRankPosition, r -> r, (a, b) -> a, HashMap::new));

        int created = 0;
        for (int i = 0; i < winners.size(); i++) {
            RoundLeaderboard winner = winners.get(i);
            Integer rank = winner.getOverallRankInRound() != null ? winner.getOverallRankInRound() : (i + 1);
            CampaignReward reward = rewardByRank.get(rank);
            if (reward == null) continue;

            if (campaignRewardDeliveryRepository.existsByCampaignRewardIdAndStudentId(
                    reward.getId(), winner.getStudent().getId())) {
                continue;
            }

            CampaignRewardDelivery delivery = new CampaignRewardDelivery();
            delivery.setCampaignReward(reward);
            delivery.setCampaign(campaign);
            delivery.setCampaignRound(finalRound);
            delivery.setRoundLeaderboard(winner);
            delivery.setStudent(winner.getStudent());
            delivery.setSchool(winner.getSchool());
            delivery.setLeaderboardRank(rank);
            delivery.setStatus(PartnershipRewardStatus.PREPARING);
            delivery.setPreparingAt(OffsetDateTime.now());
            CampaignRewardDelivery saved = campaignRewardDeliveryRepository.save(delivery);
            created++;

            notifyWinnerPreparing(saved, campaign, winner.getStudent(), winner.getSchool(), rank, reward);
        }

        log.info("[CampaignScheduler] Campaign '{}': da tao {} delivery record(s) cho nguoi thang cuoc",
                campaign.getCampaignName(), created);
    }

    private void notifyWinnerPreparing(CampaignRewardDelivery delivery, Campaign campaign,
                                       Student student, School school, int rank, CampaignReward reward) {
        String campaignName = campaign.getCampaignName();
        String rewardName = reward.getRewardName();
        String studentName = student.getFullName();
        String schoolName = school.getSchoolName();
        UUID deliveryId = delivery.getId();

        // Thong bao hoc sinh dat giai
        eventPublisher.publishEvent(NotificationEvent.builder()
                .source(this)
                .recipientUserId(student.getUser().getId())
                .type(NotificationType.PARTNERSHIP_REWARD_PREPARING)
                .title("Chuc mung! Ban dat giai hang " + rank + "!")
                .message("Ban xep hang " + rank + " tai chien dich \"" + campaignName
                        + "\". Phan thuong \"" + rewardName + "\" dang duoc chuan bi gui ve truong ban.")
                .referenceType("campaign_reward_delivery")
                .referenceId(deliveryId)
                .sendEmail(false)
                .build());

        // Thong bao phu huynh cua hoc sinh
        List<StudentParentLink> parentLinks = studentParentLinkRepository.findByStudentId(student.getId());
        for (StudentParentLink link : parentLinks) {
            eventPublisher.publishEvent(NotificationEvent.builder()
                    .source(this)
                    .recipientUserId(link.getParent().getUser().getId())
                    .type(NotificationType.PARTNERSHIP_REWARD_PREPARING)
                    .title("Con ban dat giai hang " + rank + "!")
                    .message(studentName + " xep hang " + rank + " tai chien dich \"" + campaignName
                            + "\". Phan thuong \"" + rewardName + "\" dang duoc chuan bi gui ve truong.")
                    .referenceType("campaign_reward_delivery")
                    .referenceId(deliveryId)
                    .sendEmail(false)
                    .build());
        }

        // Thong bao truong
        eventPublisher.publishEvent(NotificationEvent.builder()
                .source(this)
                .recipientUserId(school.getUser().getId())
                .type(NotificationType.PARTNERSHIP_REWARD_PREPARING)
                .title("Hoc sinh truong ban dat giai tai chien dich partnership")
                .message("Hoc sinh " + studentName + " cua " + schoolName + " xep hang " + rank
                        + " tai chien dich \"" + campaignName
                        + "\". Phan thuong dang duoc chuan bi, vui long cho qua ve truong.")
                .referenceType("campaign_reward_delivery")
                .referenceId(deliveryId)
                .sendEmail(false)
                .build());
    }

    // ======================== Auto-reject helpers ========================

    // Tu dong tu choi tat ca truong con INVITED (chua accept/reject) truoc khi campaign chuyen INVITING
    // Dung cho: Partnership JOINING → INVITING
    private void autoRejectPendingSchoolInvitations(Campaign campaign) {
        List<CampaignSchoolParticipate> pendingInvited =
                campaignSchoolParticipateRepository.findByCampaignIdAndStatus(
                        campaign.getId(), ParticipationStatus.INVITED);

        if (pendingInvited.isEmpty()) return;

        for (CampaignSchoolParticipate sp : pendingInvited) {
            sp.setStatus(ParticipationStatus.REJECTED);
            campaignSchoolParticipateRepository.save(sp);
            log.info("[CampaignScheduler] Auto-rejected school invitation: school={}, campaign={}",
                    sp.getSchool().getUser().getEmail(), campaign.getCampaignCode());
        }

        // Thong bao cac truong bi tu choi tu dong
        List<User> rejectedSchoolUsers = pendingInvited.stream()
                .map(sp -> sp.getSchool().getUser())
                .distinct()
                .toList();

        notificationService.notifyUsers(
                rejectedSchoolUsers,
                NotificationType.CAMPAIGN_INVITE,
                "Lời mời tham gia đã hết hạn",
                "Thời hạn phản hồi lời mời tham gia chiến dịch \""
                        + campaign.getCampaignName()
                        + "\" đã kết thúc. Lời mời của trường đã bị hủy tự động.",
                "campaign",
                campaign.getId(),
                null,
                false
        );

        log.info("[CampaignScheduler] Auto-rejected {} school invitation(s) for campaign '{}'",
                pendingInvited.size(), campaign.getCampaignCode());
    }

    // Tu dong tu choi tat ca participant PREPARED (cho phu huynh duyet) truoc khi campaign chuyen ON_GOING
    // Dung cho: School INVITING/EXTENDED → ON_GOING va Partnership INVITING → ON_GOING
    private void autoRejectPendingParentApprovals(Campaign campaign) {
        List<CampaignParticipant> pendingParents = campaignParticipantRepository
                .findByCampaignIdAndParentApprovalStatusAndIsActiveTrue(
                        campaign.getId(), ParticipationStatus.PREPARED);

        if (pendingParents.isEmpty()) return;

        for (CampaignParticipant p : pendingParents) {
            p.setParentApprovalStatus(ParticipationStatus.REJECTED);
            campaignParticipantRepository.save(p);
            log.info("[CampaignScheduler] Auto-rejected parent approval: student={}, campaign={}",
                    p.getStudent().getStudentCode(), campaign.getCampaignCode());
        }

        // Thong bao hoc sinh bi tu choi tu dong
        List<User> studentUsers = pendingParents.stream()
                .map(p -> p.getStudent().getUser())
                .distinct()
                .toList();

        notificationService.notifyUsers(
                studentUsers,
                NotificationType.CAMPAIGN_JOINING,
                "Lời mời tham gia chiến dịch đã hết hạn",
                "Thời hạn phụ huynh xét duyệt cho chiến dịch \""
                        + campaign.getCampaignName()
                        + "\" đã kết thúc. Bạn không thể tham gia chiến dịch này.",
                "campaign",
                campaign.getId(),
                null,
                false
        );

        // Thong bao phu huynh cua cac hoc sinh bi tu choi
        List<User> parentUsers = pendingParents.stream()
                .flatMap(p -> studentParentLinkRepository
                        .findByStudentId(p.getStudent().getId())
                        .stream()
                        .map(StudentParentLink::getParent)
                        .map(parent -> parent.getUser()))
                .distinct()
                .toList();

        if (!parentUsers.isEmpty()) {
            notificationService.notifyUsers(
                    parentUsers,
                    NotificationType.CAMPAIGN_JOINING,
                    "Lời mời tham gia chiến dịch đã hết hạn",
                    "Thời hạn xét duyệt cho chiến dịch \""
                            + campaign.getCampaignName()
                            + "\" đã kết thúc. Lời mời của con bạn đã bị hủy tự động.",
                    "campaign",
                    campaign.getId(),
                    null,
                    false
            );
        }

        log.info("[CampaignScheduler] Auto-rejected {} pending parent approval(s) for campaign '{}'",
                pendingParents.size(), campaign.getCampaignCode());
    }

    /**
     * Tự động reject tất cả invitation chưa được APPROVED khi campaign chuyển sang ON_GOING.
     * Lý do: phụ huynh không phản hồi trong thời gian được mời.
     */
    private void autoRejectPendingInvitations(Campaign campaign) {
        List<CampaignParticipant> pendingParticipants = campaignParticipantRepository
                .findByCampaignIdAndParentApprovalStatusNotAndIsActiveTrue(
                        campaign.getId(), ParticipationStatus.APPROVED);

        if (pendingParticipants.isEmpty()) return;

        OffsetDateTime now = OffsetDateTime.now();
        for (CampaignParticipant participant : pendingParticipants) {
            participant.setParentApprovalStatus(ParticipationStatus.REJECTED);
            participant.setRejectionReason("Tự động từ chối vì không phản hồi trong thời gian được mời");
            participant.setParentApprovedAt(now);
            campaignParticipantRepository.save(participant);
        }

        log.info("[CampaignScheduler] Auto-rejected {} pending invitation(s) for campaign '{}'",
                pendingParticipants.size(), campaign.getCampaignName());
    }
}
