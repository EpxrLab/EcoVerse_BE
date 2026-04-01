package com.sep490.ecoverse_be.scheduler;

import com.sep490.ecoverse_be.entity.RewardRequest;
import com.sep490.ecoverse_be.entity.StudentParentLink;
import com.sep490.ecoverse_be.enums.NotificationType;
import com.sep490.ecoverse_be.enums.RewardRequestStatus;
import com.sep490.ecoverse_be.event.NotificationEvent;
import com.sep490.ecoverse_be.repository.RewardRequestRepository;
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
public class RewardRequestScheduler {

    private final RewardRequestRepository rewardRequestRepository;
    private final StudentParentLinkRepository studentParentLinkRepository;
    private final ApplicationEventPublisher eventPublisher;

    // Chay moi ngay luc 2:00 SA
    // Tu dong confirm cac yeu cau DELIVERED qua 7 ngay ma phu huynh chua xac nhan
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void autoConfirmExpiredDeliveries() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(7);

        List<RewardRequest> expiredRequests = rewardRequestRepository
                .findByStatusAndDeliveredAtBefore(RewardRequestStatus.DELIVERED, deadline);

        if (expiredRequests.isEmpty()) {
            return;
        }

        log.info("[RewardRequestScheduler] Auto-confirming {} expired DELIVERED request(s)...", expiredRequests.size());

        for (RewardRequest request : expiredRequests) {
            request.setStatus(RewardRequestStatus.CONFIRMED);
            request.setConfirmedAt(LocalDateTime.now());
            // confirmedByParent = null → he thong tu dong confirm
            rewardRequestRepository.save(request);

            log.info("[RewardRequestScheduler] Auto-confirmed: requestCode={}, student={}, reward={}",
                    request.getRequestCode(),
                    request.getStudent().getFullName(),
                    request.getReward().getRewardName());

            // Thong bao truong: he thong tu dong xac nhan do qua han
            eventPublisher.publishEvent(NotificationEvent.builder()
                    .recipientUserId(request.getSchool().getUser().getId())
                    .type(NotificationType.REWARD_DELIVERED)
                    .title("Quà đã tự động xác nhận")
                    .message("Yêu cầu \"" + request.getReward().getRewardName() + "\" của "
                            + request.getStudent().getFullName()
                            + " đã được tự động xác nhận do phụ huynh không xác nhận trong 7 ngày. Mã: "
                            + request.getRequestCode())
                    .referenceType("reward_request")
                    .referenceId(request.getId())
                    .sendEmail(false)
                    .build());

            // Thong bao phu huynh: he thong tu dong xac nhan
            List<StudentParentLink> parentLinks = studentParentLinkRepository
                    .findByStudentId(request.getStudent().getId());
            for (StudentParentLink link : parentLinks) {
                eventPublisher.publishEvent(NotificationEvent.builder()
                        .recipientUserId(link.getParent().getUser().getId())
                        .type(NotificationType.REWARD_DELIVERED)
                        .title("Quà đã được tự động xác nhận")
                        .message("Do không xác nhận trong 7 ngày, quà \""
                                + request.getReward().getRewardName() + "\" của "
                                + request.getStudent().getFullName()
                                + " đã được hệ thống tự động xác nhận.")
                        .referenceType("reward_request")
                        .referenceId(request.getId())
                        .sendEmail(false)
                        .build());
            }
        }

        log.info("[RewardRequestScheduler] Auto-confirm completed: {} request(s) confirmed.", expiredRequests.size());
    }
}
