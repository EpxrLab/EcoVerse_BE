package com.sep490.ecoverse_be.scheduler;

import com.sep490.ecoverse_be.entity.Subscription;
import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.enums.NotificationType;
import com.sep490.ecoverse_be.enums.SubscriberType;
import com.sep490.ecoverse_be.enums.SubscriptionStatus;
import com.sep490.ecoverse_be.event.NotificationEvent;
import com.sep490.ecoverse_be.repository.SubscriptionPlanRepository;
import com.sep490.ecoverse_be.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    // Dung ApplicationEventPublisher thay vi inject truc tiep NotificationService
    // -> tuan thu best practice event-driven, tach biet concern
    private final ApplicationEventPublisher eventPublisher;

    // Chay moi ngay luc 8:00 SA: canh bao subscription sap het han trong 7 ngay
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkExpiringSubscriptions() {
        log.info("Running subscription expiry check...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysLater = now.plusDays(7);

        List<Subscription> expiringSubscriptions = subscriptionRepository
                .findByStatusAndEndDateBetween(SubscriptionStatus.ACTIVE, now, sevenDaysLater);

        for (Subscription subscription : expiringSubscriptions) {
            User owner = getSubscriptionOwner(subscription);
            if (owner == null) continue;

            long daysLeft = ChronoUnit.DAYS.between(now.toLocalDate(), subscription.getEndDate().toLocalDate());
            String planName = subscription.getPlan().getPlanName();

            // Publish event thay vi goi service truc tiep
            eventPublisher.publishEvent(NotificationEvent.builder()
                    .recipientUserId(owner.getId())
                    .type(NotificationType.SUBSCRIPTION_EXPIRING)
                    .title("Gói đăng ký sắp hết hạn")
                    .message("Gói đăng ký \"" + planName + "\" của bạn sẽ hết hạn sau " + daysLeft
                            + " ngày, vào ngày " + subscription.getEndDate().toLocalDate()
                            + ". Vui lòng gia hạn để tiếp tục sử dụng tính năng cao cấp.")
                    .referenceType("subscription")
                    .referenceId(subscription.getId())
                    .metadata(Map.of(
                            "daysLeft", daysLeft,
                            "planName", planName,
                            "endDate", subscription.getEndDate().toString()
                    ))
                    .sendEmail(true)
                    .build());

            log.info("Expiry warning published for subscription {} (expires in {} days)",
                    subscription.getSubscriptionCode(), daysLeft);
        }

        log.info("Expiry check completed. {} warning(s) published.", expiringSubscriptions.size());
    }

    // Chay moi ngay luc 0:05 SA: danh dau subscription het han va gui thong bao
    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void expireSubscriptions() {
        log.info("Running subscription expiration job...");

        LocalDateTime now = LocalDateTime.now();

        List<Subscription> expiredSubscriptions = subscriptionRepository.findExpiredSubscriptions(now);

        for (Subscription subscription : expiredSubscriptions) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);

            User owner = getSubscriptionOwner(subscription);
            if (owner != null) {
                eventPublisher.publishEvent(NotificationEvent.builder()
                        .recipientUserId(owner.getId())
                        .type(NotificationType.SUBSCRIPTION_EXPIRED)
                        .title("Gói đăng ký đã hết hạn")
                        .message("Gói đăng ký \"" + subscription.getPlan().getPlanName()
                                + "\" đã hết hạn. Vui lòng gia hạn để tiếp tục sử dụng tính năng cao cấp.")
                        .referenceType("subscription")
                        .referenceId(subscription.getId())
                        .metadata(Map.of(
                                "planName", subscription.getPlan().getPlanName(),
                                "expiredAt", subscription.getEndDate().toString()
                        ))
                        .sendEmail(true)
                        .build());
            }

            // Tự động gán gói miễn phí sau khi hết hạn
            autoAssignFreePlan(subscription);

            log.info("Subscription {} marked as expired", subscription.getSubscriptionCode());
        }

        log.info("Expiration job completed. {} subscription(s) expired.", expiredSubscriptions.size());
    }

    // Chay moi ngay luc 1:00 SA: huy subscription PENDING qua 24 gio
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void cancelStalePendingSubscriptions() {
        log.info("Running stale pending subscription cleanup...");

        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        List<Subscription> staleSubscriptions = subscriptionRepository
                .findByStatusAndEndDateBetween(SubscriptionStatus.PENDING_RENEWAL,
                        LocalDateTime.of(2000, 1, 1, 0, 0), cutoff);

        int count = 0;
        for (Subscription subscription : staleSubscriptions) {
            if (subscription.getCreatedAt() != null && subscription.getCreatedAt().isBefore(cutoff)) {
                subscription.setStatus(SubscriptionStatus.CANCELLED);
                subscription.setCancellationReason("Payment not received within 24 hours");
                subscription.setCancelledAt(LocalDateTime.now());
                subscriptionRepository.save(subscription);
                count++;
            }
        }

        log.info("Stale pending cleanup completed. {} subscription(s) cancelled.", count);
    }

    // Lay User chu so huu cua subscription (School hoac Partnership)
    private User getSubscriptionOwner(Subscription subscription) {
        if (subscription.getSubscriberType() == SubscriberType.SCHOOL && subscription.getSchool() != null) {
            return subscription.getSchool().getUser();
        } else if (subscription.getSubscriberType() == SubscriberType.PARTNERSHIP && subscription.getPartnership() != null) {
            return subscription.getPartnership().getUser();
        }
        return null;
    }

    // Tự động gán gói miễn phí sau khi subscription hết hạn
    private void autoAssignFreePlan(Subscription expiredSubscription) {
        SubscriberType type = expiredSubscription.getSubscriberType();

        // Kiểm tra xem đã có ACTIVE subscription chưa (user có thể đã tự nâng gói trước khi scheduler chạy)
        boolean alreadyActive;
        if (type == SubscriberType.SCHOOL && expiredSubscription.getSchool() != null) {
            alreadyActive = subscriptionRepository
                    .findBySchoolIdAndStatus(expiredSubscription.getSchool().getId(), SubscriptionStatus.ACTIVE)
                    .isPresent();
        } else if (type == SubscriberType.PARTNERSHIP && expiredSubscription.getPartnership() != null) {
            alreadyActive = subscriptionRepository
                    .findByPartnershipIdAndStatus(expiredSubscription.getPartnership().getId(), SubscriptionStatus.ACTIVE)
                    .isPresent();
        } else {
            return;
        }

        if (alreadyActive) {
            return;
        }

        // Tìm gói miễn phí phù hợp
        var freePlanOpt = subscriptionPlanRepository.findActiveFreeBySubscriberType(type);
        if (freePlanOpt.isEmpty()) {
            log.warn("Không tìm thấy gói miễn phí cho type={}, bỏ qua tự động gán.", type);
            return;
        }

        var freePlan = freePlanOpt.get();
        Subscription freeSub = new Subscription();
        freeSub.setSubscriptionCode("SUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        freeSub.setSubscriberType(type);
        freeSub.setSchool(expiredSubscription.getSchool());
        freeSub.setPartnership(expiredSubscription.getPartnership());
        freeSub.setPlan(freePlan);
        freeSub.setStatus(SubscriptionStatus.ACTIVE);
        freeSub.setStartDate(LocalDateTime.now());
        freeSub.setEndDate(LocalDateTime.now().plusDays(freePlan.getDurationDays()));
        freeSub.setRenewedFrom(expiredSubscription);
        subscriptionRepository.save(freeSub);

        log.info("Đã tự động gán gói miễn phí '{}' cho subscription hết hạn {}",
                freePlan.getPlanName(), expiredSubscription.getSubscriptionCode());

        User owner = getSubscriptionOwner(expiredSubscription);
        if (owner != null) {
            eventPublisher.publishEvent(NotificationEvent.builder()
                    .recipientUserId(owner.getId())
                    .type(NotificationType.SUBSCRIPTION_EXPIRED)
                    .title("Đã chuyển về gói miễn phí")
                    .message("Gói đăng ký \"" + expiredSubscription.getPlan().getPlanName()
                            + "\" đã hết hạn. Bạn đã được tự động chuyển về gói miễn phí \""
                            + freePlan.getPlanName() + "\". Vui lòng nâng cấp để tiếp tục sử dụng tính năng cao cấp.")
                    .referenceType("subscription")
                    .referenceId(freeSub.getId())
                    .sendEmail(true)
                    .build());
        }
    }
}
