package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.response.PaymentResponse;
import com.sep490.ecoverse_be.entity.*;
import com.sep490.ecoverse_be.enums.*;
import com.sep490.ecoverse_be.exception.FuncErrorException;
import com.sep490.ecoverse_be.exception.ResourceNotFoundException;
import com.sep490.ecoverse_be.mapper.PaymentMapper;
import com.sep490.ecoverse_be.event.NotificationEvent;
import com.sep490.ecoverse_be.repository.PaymentRepository;
import com.sep490.ecoverse_be.repository.SubscriptionRepository;
import com.sep490.ecoverse_be.service.IPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.webhooks.WebhookData;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements IPaymentService {

    private final PayOS payOS;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    // Dùng ApplicationEventPublisher thay vi INotificationService trực tiếp -> event-driven
    private final ApplicationEventPublisher eventPublisher;
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
            payment.setFailedAt(OffsetDateTime.now());
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
                payment.setPaidAt(OffsetDateTime.now());
                paymentRepository.save(payment);

                // Activate subscription and retire previous FREE subscription if this is an upgrade flow.
                Subscription subscription = payment.getSubscription();
                Subscription previousSubscription = subscription.getRenewedFrom();
                if (previousSubscription != null && previousSubscription.getStatus() == SubscriptionStatus.ACTIVE) {
                    previousSubscription.setStatus(SubscriptionStatus.CANCELLED);
                    previousSubscription.setCancellationReason("Upgraded to plan " + subscription.getPlan().getPlanName());
                    previousSubscription.setCancelledAt(OffsetDateTime.now());
                    previousSubscription.setEndDate(OffsetDateTime.now());
                    subscriptionRepository.save(previousSubscription);
                }

                subscription.setStatus(SubscriptionStatus.ACTIVE);
                subscription.setStartDate(OffsetDateTime.now());
                subscription.setEndDate(OffsetDateTime.now().plusDays(subscription.getPlan().getDurationDays()));
                subscriptionRepository.save(subscription);

                // Publish event để NotificationService xử lý bất đồng bộ qua listener
                User recipient = getSubscriptionOwner(subscription);
                if (recipient != null) {
                    eventPublisher.publishEvent(NotificationEvent.builder()
                            .recipientUserId(recipient.getId())
                            .type(NotificationType.SYSTEM_ANNOUNCEMENT)
                            .title("Kích hoạt gói đăng ký thành công")
                            .message("Gói đăng ký \"" + subscription.getPlan().getPlanName()
                                    + "\" của bạn đã được kích hoạt thành công. Hiệu lực đến ngày "
                                    + subscription.getEndDate().toLocalDate() + ".")
                            .referenceType("subscription")
                            .referenceId(subscription.getId())
                            .sendEmail(true)
                            .build());
                }

                log.info("Subscription {} activated after payment {}", subscription.getSubscriptionCode(), payment.getPaymentCode());

            } else {
                // Payment failed/cancelled
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailedAt(OffsetDateTime.now());
                payment.setFailureReason("PayOS code: " + code);
                paymentRepository.save(payment);

                // Cancel the pending subscription
                Subscription subscription = payment.getSubscription();
                if (subscription.getStatus() == SubscriptionStatus.PENDING_RENEWAL) {
                    subscription.setStatus(SubscriptionStatus.CANCELLED);
                    subscription.setCancellationReason("Payment failed");
                    subscription.setCancelledAt(OffsetDateTime.now());
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thanh toán."));
        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    public void cancelPaymentByOrderCode(long orderCode) {
        Payment payment = paymentRepository.findByTransactionRef(String.valueOf(orderCode))
                .orElse(null);

        if (payment == null) {
            log.warn("cancelPaymentByOrderCode: không tìm thấy payment với orderCode={}", orderCode);
            return;
        }

        // Chỉ xử lý nếu payment đang PENDING (chưa hoàn thành hoặc đã hủy)
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info("cancelPaymentByOrderCode: payment {} trạng thái {} – bỏ qua", payment.getPaymentCode(), payment.getStatus());
            return;
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setFailedAt(OffsetDateTime.now());
        payment.setFailureReason("Người dùng hủy thanh toán");
        paymentRepository.save(payment);

        // Hủy subscription PENDING_RENEWAL liên quan; KHÔNG hủy subscription gốc (renewedFrom)
        Subscription subscription = payment.getSubscription();
        if (subscription != null && subscription.getStatus() == SubscriptionStatus.PENDING_RENEWAL) {
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            subscription.setCancellationReason("Người dùng hủy thanh toán");
            subscription.setCancelledAt(OffsetDateTime.now());
            subscriptionRepository.save(subscription);
            log.info("Đã hủy subscription PENDING_RENEWAL {} do người dùng hủy thanh toán",
                    subscription.getSubscriptionCode());
        }

        log.info("Đã hủy payment {} (orderCode={})", payment.getPaymentCode(), orderCode);
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
