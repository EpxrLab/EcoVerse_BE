package com.sep490.ecoverse_be.scheduler;

import com.sep490.ecoverse_be.entity.Campaign;
import com.sep490.ecoverse_be.enums.SchoolCampaignStatus;
import com.sep490.ecoverse_be.repository.CampaignRepository;
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

    // SCHEDULED → INVITING: khi invitationDate đã đến
    private void transitionToInviting(LocalDateTime now) {
        List<Campaign> campaigns = campaignRepository.findSchoolCampaignsReadyForInviting(now);
        if (campaigns.isEmpty()) return;

        for (Campaign campaign : campaigns) {
            campaign.setSchoolStatus(SchoolCampaignStatus.INVITING);
            campaignRepository.save(campaign);
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
            campaign.setSchoolStatus(SchoolCampaignStatus.ON_GOING);
            campaignRepository.save(campaign);
            log.info("[CampaignScheduler] Campaign '{}' ({}): {} → ON_GOING",
                    campaign.getCampaignName(), campaign.getCampaignCode(), campaign.getSchoolStatus());
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
        }
        log.info("[CampaignScheduler] {} campaign(s) transitioned to COMPLETED", campaigns.size());
    }
}
