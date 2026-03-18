package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.response.PaymentResponse;
import com.sep490.ecoverse_be.entity.*;
import com.sep490.ecoverse_be.enums.*;
import com.sep490.ecoverse_be.exception.FuncErrorException;
import com.sep490.ecoverse_be.exception.ResourceNotFoundException;
import com.sep490.ecoverse_be.mapper.PaymentMapper;
import com.sep490.ecoverse_be.repository.PaymentRepository;
import com.sep490.ecoverse_be.repository.SubscriptionRepository;
import com.sep490.ecoverse_be.service.INotificationService;
import com.sep490.ecoverse_be.service.IPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.webhooks.WebhookData;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements IPaymentService {

    private final PayOS payOS;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final INotificationService notificationService;
    private final PaymentMapper paymentMapper;

    @Value("${payos.return-url}")
    private String returnUrl;

    @Value("${payos.cancel-url}")
    private String cancelUrl;

    @Override
    @Transactional
    public PaymentResponse createPayment(Subscription subscription, User user) {
        SubscriptionPlan plan = subscription.getPlan();

        // Generate unique order code for PayOS (must be positive long)
        long orderCode = System.currentTimeMillis() / 1000;

        // Create payment record
        Payment payment = new Payment();
        payment.setPaymentCode("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setSubscriberType(subscription.getSubscriberType());
        payment.setSchool(subscription.getSchool());
        payment.setPartnership(subscription.getPartnership());
        payment.setSubscription(subscription);
        payment.setAmount(plan.getPrice());
        payment.setCurrency(plan.getCurrency());
        payment.setPaymentMethod(PaymentMethod.PAYOS);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionRef(String.valueOf(orderCode));
        payment.setPayerEmail(user.getEmail());
        payment.setCreatedBy(user);

        payment = paymentRepository.save(payment);

        // Create PayOS checkout
        try {
            String description = "EcoVerse - " + plan.getPlanName();
            // PayOS description max 25 chars
            if (description.length() > 25) {
                description = description.substring(0, 25);
            }

            PaymentLinkItem item = PaymentLinkItem.builder()
                    .name(plan.getPlanName())
                    .quantity(1)
                    .price(plan.getPrice().longValue())
                    .build();

            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(plan.getPrice().longValue())
                    .description(description)
                    .returnUrl(returnUrl + "?orderCode=" + orderCode)
                    .cancelUrl(cancelUrl + "?orderCode=" + orderCode)
                    .item(item)
                    .build();

            CreatePaymentLinkResponse checkoutResponse = payOS.paymentRequests().create(paymentData);

            String checkoutUrl = checkoutResponse.getCheckoutUrl();

            log.info("PayOS payment created: orderCode={}, checkoutUrl={}", orderCode, checkoutUrl);

            return paymentMapper.toResponse(payment, checkoutUrl);

        } catch (Exception e) {
            log.error("Failed to create PayOS payment link", e);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailedAt(LocalDateTime.now());
            payment.setFailureReason("Failed to create payment link: " + e.getMessage());
            paymentRepository.save(payment);
            throw new FuncErrorException("Failed to create payment. Please try again later.");
        }
    }

    @Override
    @Transactional
    public void handlePayOSWebhook(Object webhookBody) {
        try {
            WebhookData data = payOS.webhooks().verify(webhookBody);

            String orderCodeStr = String.valueOf(data.getOrderCode());
            log.info("PayOS webhook received: orderCode={}, code={}", orderCodeStr, data.getCode());

            Payment payment = paymentRepository.findByTransactionRef(orderCodeStr)
                    .orElse(null);

            if (payment == null) {
                log.warn("Payment not found for orderCode: {}", orderCodeStr);
                return;
            }

            // Already processed
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                log.info("Payment {} already completed, skipping.", payment.getPaymentCode());
                return;
            }

            String code = data.getCode();

            if ("00".equals(code)) {
                // Payment successful
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);

                // Activate subscription and retire previous FREE subscription if this is an upgrade flow.
                Subscription subscription = payment.getSubscription();
                Subscription previousSubscription = subscription.getRenewedFrom();
                if (previousSubscription != null && previousSubscription.getStatus() == SubscriptionStatus.ACTIVE) {
                    previousSubscription.setStatus(SubscriptionStatus.CANCELLED);
                    previousSubscription.setCancellationReason("Upgraded to plan " + subscription.getPlan().getPlanName());
                    previousSubscription.setCancelledAt(LocalDateTime.now());
                    previousSubscription.setEndDate(LocalDateTime.now());
                    subscriptionRepository.save(previousSubscription);
                }

                subscription.setStatus(SubscriptionStatus.ACTIVE);
                subscription.setStartDate(LocalDateTime.now());
                subscription.setEndDate(LocalDateTime.now().plusDays(subscription.getPlan().getDurationDays()));
                subscriptionRepository.save(subscription);

                // Send notification
                User recipient = getSubscriptionOwner(subscription);
                if (recipient != null) {
                    notificationService.sendNotification(
                            recipient,
                            NotificationType.SYSTEM_ANNOUNCEMENT,
                            "Subscription Activated",
                            "Your subscription to " + subscription.getPlan().getPlanName()
                                    + " has been activated successfully. Valid until "
                                    + subscription.getEndDate().toLocalDate(),
                            "subscription",
                            subscription.getId(),
                            null
                    );
                }

                log.info("Subscription {} activated after payment {}", subscription.getSubscriptionCode(), payment.getPaymentCode());

            } else {
                // Payment failed/cancelled
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailedAt(LocalDateTime.now());
                payment.setFailureReason("PayOS code: " + code);
                paymentRepository.save(payment);

                // Cancel the pending subscription
                Subscription subscription = payment.getSubscription();
                if (subscription.getStatus() == SubscriptionStatus.PENDING_RENEWAL) {
                    subscription.setStatus(SubscriptionStatus.CANCELLED);
                    subscription.setCancellationReason("Payment failed");
                    subscription.setCancelledAt(LocalDateTime.now());
                    subscriptionRepository.save(subscription);
                }

                log.info("Payment {} failed with code {}", payment.getPaymentCode(), code);
            }

        } catch (Exception e) {
            log.error("Error processing PayOS webhook", e);
            throw new FuncErrorException("Webhook processing failed.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found."));
        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderCode(long orderCode) {
        Payment payment = paymentRepository.findByTransactionRef(String.valueOf(orderCode))
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found."));
        return paymentMapper.toResponse(payment);
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
