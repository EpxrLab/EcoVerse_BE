package com.sep490.ecoverse_be.scheduler;

import com.sep490.ecoverse_be.entity.Campaign;
import com.sep490.ecoverse_be.entity.CampaignParticipant;
import com.sep490.ecoverse_be.entity.CampaignSchoolParticipate;
import com.sep490.ecoverse_be.entity.StudentParentLink;
import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.enums.NotificationType;
import com.sep490.ecoverse_be.enums.ParticipationStatus;
import com.sep490.ecoverse_be.enums.PartnershipCampaignStatus;
import com.sep490.ecoverse_be.enums.SchoolCampaignStatus;
import com.sep490.ecoverse_be.repository.CampaignParticipantRepository;
import com.sep490.ecoverse_be.repository.CampaignRepository;
import com.sep490.ecoverse_be.repository.CampaignSchoolParticipateRepository;
import com.sep490.ecoverse_be.repository.StudentParentLinkRepository;
import com.sep490.ecoverse_be.service.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CampaignScheduler {

    private final CampaignRepository campaignRepository;
    private final CampaignParticipantRepository campaignParticipantRepository;
    private final CampaignSchoolParticipateRepository campaignSchoolParticipateRepository;
    private final StudentParentLinkRepository studentParentLinkRepository;
    private final INotificationService notificationService;

    /**
     * Chạy mỗi phút để tự động chuyển trạng thái campaign trường (SCHOOL_INTERNAL).
     * Flow: SCHEDULED → INVITING → ON_GOING → COMPLETED
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void autoTransitionSchoolCampaignStatuses() {
        LocalDateTime now = LocalDateTime.now();

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
        LocalDateTime now = LocalDateTime.now();
        partnershipToJoining(now);
        partnershipToInviting(now);
        partnershipToOnGoing(now);
        partnershipToCompleted(now);
    }

    // SCHEDULED → INVITING: khi invitationDate đã đến
    private void transitionToInviting(LocalDateTime now) {
        List<Campaign> campaigns = campaignRepository.findSchoolCampaignsReadyForInviting(now);
        if (campaigns.isEmpty()) return;

        for (Campaign campaign : campaigns) {
            campaign.setSchoolStatus(SchoolCampaignStatus.INVITING);
            campaignRepository.save(campaign);

            List<CampaignParticipant> pending = campaignParticipantRepository
                    .findByCampaignIdAndInvitationSentAtIsNullAndIsActiveTrue(campaign.getId());
            for (CampaignParticipant p : pending) {
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
    private void transitionToOnGoing(LocalDateTime now) {
        List<Campaign> campaigns = campaignRepository.findSchoolCampaignsReadyForOnGoing(now);
        if (campaigns.isEmpty()) return;

        for (Campaign campaign : campaigns) {
            // Truoc khi chuyen ON_GOING: tu dong tu choi tat ca PENDING_PARENT_APPROVAL con lai
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
    private void transitionToCompleted(LocalDateTime now) {
        List<Campaign> campaigns = campaignRepository.findSchoolCampaignsReadyForCompleted(now);
        if (campaigns.isEmpty()) return;

        for (Campaign campaign : campaigns) {
            campaign.setSchoolStatus(SchoolCampaignStatus.COMPLETED);
            campaignRepository.save(campaign);
            log.info("[CampaignScheduler] Campaign '{}' ({}): ON_GOING → COMPLETED",
                    campaign.getCampaignName(), campaign.getCampaignCode());

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

    private void partnershipToJoining(LocalDateTime now) {
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

    private void partnershipToInviting(LocalDateTime now) {
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

    private void partnershipToOnGoing(LocalDateTime now) {
        List<Campaign> campaigns = campaignRepository.findPartnershipCampaignsReadyForOnGoing(now);
        if (campaigns.isEmpty()) return;

        for (Campaign campaign : campaigns) {
            // Truoc khi chuyen ON_GOING: tu dong tu choi tat ca PENDING_PARENT_APPROVAL con lai
            autoRejectPendingParentApprovals(campaign);

            campaign.setPartnershipStatus(PartnershipCampaignStatus.ON_GOING);
            campaignRepository.save(campaign);

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

    private void partnershipToCompleted(LocalDateTime now) {
        List<Campaign> campaigns = campaignRepository.findPartnershipCampaignsReadyForCompleted(now);
        if (campaigns.isEmpty()) return;

        for (Campaign campaign : campaigns) {
            campaign.setPartnershipStatus(PartnershipCampaignStatus.COMPLETED);
            campaignRepository.save(campaign);

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

    // Tu dong tu choi tat ca parent PENDING_PARENT_APPROVAL truoc khi campaign chuyen ON_GOING
    // Dung cho: School INVITING/EXTENDED → ON_GOING va Partnership INVITING → ON_GOING
    private void autoRejectPendingParentApprovals(Campaign campaign) {
        List<CampaignParticipant> pendingParents = campaignParticipantRepository
                .findByCampaignIdAndParentApprovalStatusAndIsActiveTrue(
                        campaign.getId(), ParticipationStatus.PENDING_PARENT_APPROVAL);

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

        LocalDateTime now = LocalDateTime.now();
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
