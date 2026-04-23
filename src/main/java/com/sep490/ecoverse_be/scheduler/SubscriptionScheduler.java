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

    // Chay moi ngay luc 0:05 SA: ACTIVE het han → PENDING_RENEWAL (vao grace period 7 ngay)
    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void enterRenewalWindow() {
        log.info("Running subscription renewal-window job...");

        LocalDateTime now = LocalDateTime.now();

        List<Subscription> expiredSubscriptions = subscriptionRepository.findExpiredSubscriptions(now);

        for (Subscription subscription : expiredSubscriptions) {
            // ACTIVE → PENDING_RENEWAL: bat dau grace period, nguoi dung van co the gia han
            subscription.setStatus(SubscriptionStatus.PENDING_RENEWAL);
            subscriptionRepository.save(subscription);

            User owner = getSubscriptionOwner(subscription);
            if (owner != null) {
                eventPublisher.publishEvent(NotificationEvent.builder()
                        .recipientUserId(owner.getId())
                        .type(NotificationType.SUBSCRIPTION_EXPIRED)
                        .title("Gói đăng ký đã hết hạn – Vui lòng gia hạn")
                        .message("Gói đăng ký \"" + subscription.getPlan().getPlanName()
                                + "\" đã hết hạn. Bạn có 7 ngày để gia hạn trước khi bị chuyển về gói miễn phí.")
                        .referenceType("subscription")
                        .referenceId(subscription.getId())
                        .metadata(Map.of(
                                "planName", subscription.getPlan().getPlanName(),
                                "expiredAt", subscription.getEndDate().toString()
                        ))
                        .sendEmail(true)
                        .build());
            }

            log.info("Subscription {} entered renewal window (PENDING_RENEWAL)", subscription.getSubscriptionCode());
        }

        log.info("Renewal-window job completed. {} subscription(s) entered PENDING_RENEWAL.", expiredSubscriptions.size());
    }

    // Chay moi ngay luc 0:10 SA: PENDING_RENEWAL qua 7 ngay grace period → EXPIRED (luu tru)
    @Scheduled(cron = "0 10 0 * * *")
    @Transactional
    public void expireOverdueRenewalSubscriptions() {
        log.info("Running overdue-renewal expiration job...");

        // Grace period 7 ngay: neu qua 7 ngay ke tu endDate van chua gia han → EXPIRED
        LocalDateTime gracePeriodCutoff = LocalDateTime.now().minusDays(7);

        List<Subscription> overdueSubscriptions =
                subscriptionRepository.findOverdueRenewalSubscriptions(gracePeriodCutoff);

        for (Subscription subscription : overdueSubscriptions) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);
            // Chi gan goi FREE khi da het grace period va chuyen sang EXPIRED
            autoAssignFreePlan(subscription);
            log.info("Subscription {} archived as EXPIRED (past grace period)", subscription.getSubscriptionCode());
        }

        log.info("Overdue-renewal job completed. {} subscription(s) archived.", overdueSubscriptions.size());
    }

    // Chay moi ngay luc 1:00 SA: huy subscription PENDING (cho thanh toan) qua 24 gio
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void cancelStalePendingSubscriptions() {
        log.info("Running stale PENDING subscription cleanup...");

        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        List<Subscription> staleSubscriptions =
                subscriptionRepository.findStalePendingSubscriptions(cutoff);

        for (Subscription subscription : staleSubscriptions) {
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            subscription.setCancellationReason("Payment not received within 24 hours");
            subscription.setCancelledAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);
            log.info("Stale PENDING subscription {} cancelled (no payment in 24h)", subscription.getSubscriptionCode());
        }

        log.info("Stale-pending cleanup completed. {} subscription(s) cancelled.", staleSubscriptions.size());
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
