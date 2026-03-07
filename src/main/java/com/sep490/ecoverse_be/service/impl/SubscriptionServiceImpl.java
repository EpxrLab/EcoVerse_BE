package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.CreateSubscriptionRequest;
import com.sep490.ecoverse_be.dto.request.RenewSubscriptionRequest;
import com.sep490.ecoverse_be.dto.response.PageResponse;
import com.sep490.ecoverse_be.dto.response.PaymentResponse;
import com.sep490.ecoverse_be.dto.response.SubscriptionResponse;
import com.sep490.ecoverse_be.entity.*;
import com.sep490.ecoverse_be.enums.*;
import com.sep490.ecoverse_be.exception.FuncErrorException;
import com.sep490.ecoverse_be.exception.ResourceNotFoundException;
import com.sep490.ecoverse_be.mapper.PaymentMapper;
import com.sep490.ecoverse_be.mapper.SubscriptionMapper;
import com.sep490.ecoverse_be.repository.*;
import com.sep490.ecoverse_be.service.IPaymentService;
import com.sep490.ecoverse_be.service.ISubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements ISubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SchoolRepository schoolRepository;
    private final PartnershipRepository partnershipRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final IPaymentService paymentService;
    private final SubscriptionMapper subscriptionMapper;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public PaymentResponse subscribe(CreateSubscriptionRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        SubscriptionPlan plan = subscriptionPlanRepository.findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found."));

        if (!plan.isActive()) {
            throw new FuncErrorException("This subscription plan is no longer available.");
        }

        // Determine subscriber type from user role
        SubscriberType subscriberType = resolveSubscriberType(user);

        if (plan.getSubscriberType() != subscriberType) {
            throw new FuncErrorException("This plan is not available for your account type.");
        }

        // Get school or partnership
        School school = null;
        Partnership partnership = null;

        if (subscriberType == SubscriberType.SCHOOL) {
            school = schoolRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("School profile not found."));
            // Check no active subscription exists
            subscriptionRepository.findBySchoolIdAndStatus(school.getId(), SubscriptionStatus.ACTIVE)
                    .ifPresent(s -> {
                        throw new FuncErrorException("You already have an active subscription. Please wait for it to expire or cancel it first.");
                    });
        } else {
            partnership = partnershipRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Partnership profile not found."));
            subscriptionRepository.findByPartnershipIdAndStatus(partnership.getId(), SubscriptionStatus.ACTIVE)
                    .ifPresent(s -> {
                        throw new FuncErrorException("You already have an active subscription. Please wait for it to expire or cancel it first.");
                    });
        }

        // Create subscription
        Subscription subscription = new Subscription();
        subscription.setSubscriptionCode(generateSubscriptionCode());
        subscription.setSubscriberType(subscriberType);
        subscription.setSchool(school);
        subscription.setPartnership(partnership);
        subscription.setPlan(plan);
        subscription.setStartDate(LocalDateTime.now());
        subscription.setEndDate(LocalDateTime.now().plusDays(plan.getDurationDays()));

        // Free plan: activate immediately
        if (plan.getPrice().compareTo(BigDecimal.ZERO) == 0) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(subscription);
            log.info("Free subscription activated for user {}: plan={}", user.getEmail(), plan.getPlanCode());
            return paymentMapper.toResponse(
                    createFreePaymentRecord(subscription, subscriberType, school, partnership, user), null);
        }

        // Paid plan: set PENDING_RENEWAL until payment confirmed
        subscription.setStatus(SubscriptionStatus.PENDING_RENEWAL);
        subscription = subscriptionRepository.save(subscription);

        // Create PayOS payment
        return paymentService.createPayment(subscription, user);
    }

    @Override
    @Transactional
    public PaymentResponse renewSubscription(RenewSubscriptionRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        Subscription oldSubscription = subscriptionRepository.findById(request.subscriptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found."));

        // Only expired or pending_renewal subscriptions can be renewed
        if (oldSubscription.getStatus() != SubscriptionStatus.EXPIRED
                && oldSubscription.getStatus() != SubscriptionStatus.PENDING_RENEWAL) {
            throw new FuncErrorException("Only expired subscriptions can be renewed.");
        }

        // Verify ownership
        verifyOwnership(oldSubscription, userId);

        // Determine plan (keep same or switch)
        SubscriptionPlan plan;
        if (request.planId() != null) {
            plan = subscriptionPlanRepository.findById(request.planId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found."));
            if (!plan.isActive()) {
                throw new FuncErrorException("This subscription plan is no longer available.");
            }
            if (plan.getSubscriberType() != oldSubscription.getSubscriberType()) {
                throw new FuncErrorException("This plan is not available for your account type.");
            }
        } else {
            plan = oldSubscription.getPlan();
        }

        // Create new subscription
        Subscription newSubscription = new Subscription();
        newSubscription.setSubscriptionCode(generateSubscriptionCode());
        newSubscription.setSubscriberType(oldSubscription.getSubscriberType());
        newSubscription.setSchool(oldSubscription.getSchool());
        newSubscription.setPartnership(oldSubscription.getPartnership());
        newSubscription.setPlan(plan);
        newSubscription.setRenewedFrom(oldSubscription);
        newSubscription.setStartDate(LocalDateTime.now());
        newSubscription.setEndDate(LocalDateTime.now().plusDays(plan.getDurationDays()));

        if (plan.getPrice().compareTo(BigDecimal.ZERO) == 0) {
            newSubscription.setStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(newSubscription);
            return paymentMapper.toResponse(
                    createFreePaymentRecord(newSubscription, oldSubscription.getSubscriberType(),
                            oldSubscription.getSchool(), oldSubscription.getPartnership(), user), null);
        }

        newSubscription.setStatus(SubscriptionStatus.PENDING_RENEWAL);
        newSubscription = subscriptionRepository.save(newSubscription);

        return paymentService.createPayment(newSubscription, user);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getMySubscription(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        SubscriberType subscriberType = resolveSubscriberType(user);

        Subscription subscription;
        if (subscriberType == SubscriberType.SCHOOL) {
            School school = schoolRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("School profile not found."));
            subscription = subscriptionRepository.findBySchoolIdAndStatus(school.getId(), SubscriptionStatus.ACTIVE)
                    .orElseThrow(() -> new ResourceNotFoundException("No active subscription found."));
        } else {
            Partnership partnership = partnershipRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Partnership profile not found."));
            subscription = subscriptionRepository.findByPartnershipIdAndStatus(partnership.getId(), SubscriptionStatus.ACTIVE)
                    .orElseThrow(() -> new ResourceNotFoundException("No active subscription found."));
        }

        return subscriptionMapper.toResponse(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubscriptionResponse> getMySubscriptionHistory(UUID userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        SubscriberType subscriberType = resolveSubscriberType(user);
        Page<Subscription> page;

        if (subscriberType == SubscriberType.SCHOOL) {
            School school = schoolRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("School profile not found."));
            page = subscriptionRepository.findBySchoolId(school.getId(), pageable);
        } else {
            Partnership partnership = partnershipRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Partnership profile not found."));
            page = subscriptionRepository.findByPartnershipId(partnership.getId(), pageable);
        }

        return PageResponse.from(page, subscriptionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscriptionById(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found."));
        return subscriptionMapper.toResponse(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubscriptionResponse> getAllSubscriptions(SubscriberType subscriberType,
                                                                   SubscriptionStatus status,
                                                                   String keyword,
                                                                   Pageable pageable) {
        Page<Subscription> page = subscriptionRepository.findAllWithFilters(subscriberType, status, keyword, pageable);
        return PageResponse.from(page, subscriptionMapper::toResponse);
    }

    @Override
    @Transactional
    public SubscriptionResponse cancelSubscription(UUID subscriptionId, String reason, UUID userId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found."));

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new FuncErrorException("Only active subscriptions can be cancelled.");
        }

        verifyOwnership(subscription, userId);

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancellationReason(reason);
        subscription.setCancelledAt(LocalDateTime.now());

        subscriptionRepository.save(subscription);
        log.info("Subscription {} cancelled by user {}", subscription.getSubscriptionCode(), userId);

        return subscriptionMapper.toResponse(subscription);
    }

    // ========================= Helper Methods =========================

    private SubscriberType resolveSubscriberType(User user) {
        if (user.getRole() == Role.PARTNERSHIP_SCHOOL) {
            return SubscriberType.SCHOOL;
        } else if (user.getRole() == Role.THIRD_PARTY_PARTNERSHIP) {
            return SubscriberType.PARTNERSHIP;
        }
        throw new FuncErrorException("Only School and Partnership accounts can manage subscriptions.");
    }

    private void verifyOwnership(Subscription subscription, UUID userId) {
        boolean isOwner = false;
        if (subscription.getSubscriberType() == SubscriberType.SCHOOL && subscription.getSchool() != null) {
            isOwner = subscription.getSchool().getUser().getId().equals(userId);
        } else if (subscription.getSubscriberType() == SubscriberType.PARTNERSHIP && subscription.getPartnership() != null) {
            isOwner = subscription.getPartnership().getUser().getId().equals(userId);
        }
        if (!isOwner) {
            throw new FuncErrorException("You do not have permission to manage this subscription.");
        }
    }

    private String generateSubscriptionCode() {
        return "SUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Payment createFreePaymentRecord(Subscription subscription, SubscriberType subscriberType,
                                             School school, Partnership partnership, User user) {
        Payment payment = new Payment();
        payment.setPaymentCode("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setSubscriberType(subscriberType);
        payment.setSchool(school);
        payment.setPartnership(partnership);
        payment.setSubscription(subscription);
        payment.setAmount(BigDecimal.ZERO);
        payment.setCurrency("VND");
        payment.setPaymentMethod(PaymentMethod.OTHER);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        payment.setPayerName(user.getEmail());
        payment.setPayerEmail(user.getEmail());
        payment.setCreatedBy(user);
        payment.setNotes("Free plan - no payment required");
        return paymentRepository.save(payment);
    }
}
