package com.sep490.ecoverse_be.scheduler;

import com.sep490.ecoverse_be.entity.Subscription;
import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.enums.NotificationType;
import com.sep490.ecoverse_be.enums.SubscriberType;
import com.sep490.ecoverse_be.enums.SubscriptionStatus;
import com.sep490.ecoverse_be.repository.SubscriptionRepository;
import com.sep490.ecoverse_be.service.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final INotificationService notificationService;

    /**
     * Runs every day at 8:00 AM.
     * Checks for subscriptions expiring within 7 days and sends warning notifications.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkExpiringSubscriptions() {
        log.info("Running subscription expiry check...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysLater = now.plusDays(7);

        // Find active subscriptions expiring within 7 days
        List<Subscription> expiringSubscriptions = subscriptionRepository
                .findByStatusAndEndDateBetween(SubscriptionStatus.ACTIVE, now, sevenDaysLater);

        for (Subscription subscription : expiringSubscriptions) {
            User owner = getSubscriptionOwner(subscription);
            if (owner == null) continue;

            long daysLeft = ChronoUnit.DAYS.between(now.toLocalDate(), subscription.getEndDate().toLocalDate());
            String planName = subscription.getPlan().getPlanName();

            notificationService.sendNotification(
                    owner,
                    NotificationType.SUBSCRIPTION_EXPIRING,
                    "Subscription Expiring Soon",
                    "Your subscription to \"" + planName + "\" will expire in " + daysLeft
                            + " day(s) on " + subscription.getEndDate().toLocalDate()
                            + ". Please renew to continue using premium features.",
                    "subscription",
                    subscription.getId(),
                    Map.of(
                            "daysLeft", daysLeft,
                            "planName", planName,
                            "endDate", subscription.getEndDate().toString()
                    )
            );

            log.info("Expiry warning sent for subscription {} (expires in {} days)",
                    subscription.getSubscriptionCode(), daysLeft);
        }

        log.info("Expiry check completed. {} warning(s) sent.", expiringSubscriptions.size());
    }

    /**
     * Runs every day at 0:05 AM.
     * Marks expired subscriptions and sends expiration notifications.
     */
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
                notificationService.sendNotification(
                        owner,
                        NotificationType.SUBSCRIPTION_EXPIRED,
                        "Subscription Expired",
                        "Your subscription to \"" + subscription.getPlan().getPlanName()
                                + "\" has expired. Please renew to continue using premium features.",
                        "subscription",
                        subscription.getId(),
                        Map.of(
                                "planName", subscription.getPlan().getPlanName(),
                                "expiredAt", subscription.getEndDate().toString()
                        )
                );
            }

            log.info("Subscription {} marked as expired", subscription.getSubscriptionCode());
        }

        log.info("Expiration job completed. {} subscription(s) expired.", expiredSubscriptions.size());
    }

    /**
     * Runs every day at 1:00 AM.
     * Cancels pending subscriptions that have been awaiting payment for more than 24 hours.
     */
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

    private User getSubscriptionOwner(Subscription subscription) {
        if (subscription.getSubscriberType() == SubscriberType.SCHOOL && subscription.getSchool() != null) {
            return subscription.getSchool().getUser();
        } else if (subscription.getSubscriberType() == SubscriberType.PARTNERSHIP && subscription.getPartnership() != null) {
            return subscription.getPartnership().getUser();
        }
        return null;
    }
}
