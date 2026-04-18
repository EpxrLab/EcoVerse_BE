package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.CreateSubscriptionRequest;
import com.sep490.ecoverse_be.dto.request.RenewSubscriptionRequest;
import com.sep490.ecoverse_be.dto.response.PageResponse;
import com.sep490.ecoverse_be.dto.response.PaymentResponse;
import com.sep490.ecoverse_be.dto.response.SubscriptionResponse;
import com.sep490.ecoverse_be.dto.response.SubscriptionTransactionResponse;
import com.sep490.ecoverse_be.entity.*;
import com.sep490.ecoverse_be.enums.*;
import com.sep490.ecoverse_be.exception.FuncErrorException;
import com.sep490.ecoverse_be.exception.ResourceNotFoundException;
import com.sep490.ecoverse_be.mapper.PaymentMapper;
import com.sep490.ecoverse_be.mapper.SubscriptionMapper;
import com.sep490.ecoverse_be.repository.*;
import com.sep490.ecoverse_be.service.IPaymentService;
import com.sep490.ecoverse_be.service.ISubscriptionService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final StudentRepository studentRepository;
    private final CampaignRepository campaignRepository;
    private final AiGenerationLogRepository aiGenerationLogRepository;

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
        boolean requestedPlanIsFree = isFreePlan(plan);

        if (plan.getSubscriberType() != subscriberType) {
            throw new FuncErrorException("This plan is not available for your account type.");
        }

        // Get school or partnership
        School school = null;
        Partnership partnership = null;
        Optional<Subscription> activeSubscriptionOptional;

        if (subscriberType == SubscriberType.SCHOOL) {
            school = schoolRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("School profile not found."));
            activeSubscriptionOptional = subscriptionRepository.findBySchoolIdAndStatus(school.getId(), SubscriptionStatus.ACTIVE);
        } else {
            partnership = partnershipRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Partnership profile not found."));
            activeSubscriptionOptional = subscriptionRepository.findByPartnershipIdAndStatus(partnership.getId(), SubscriptionStatus.ACTIVE);
        }

        Subscription activeSubscription = activeSubscriptionOptional.orElse(null);
        if (activeSubscription != null) {
            boolean activePlanIsFree = isFreePlan(activeSubscription.getPlan());

            if (activePlanIsFree && !requestedPlanIsFree) {
                // Free -> Paid: cho phép nâng gói
            } else if (!activePlanIsFree && requestedPlanIsFree) {
                // Paid -> Free: kiểm tra ràng buộc số học sinh trước khi hạ gói
                validateDowngradeToFree(school, plan);
            } else if (!activePlanIsFree && !requestedPlanIsFree) {
                // Paid -> Paid: chỉ cho phép nâng lên gói cao hơn
                if (plan.getPrice().compareTo(activeSubscription.getPlan().getPrice()) <= 0) {
                    throw new FuncErrorException(
                            "Chỉ được phép nâng cấp lên gói có giá cao hơn. Không thể đăng ký gói thấp hơn hoặc bằng gói hiện tại.");
                }
            } else {
                // Free -> Free: đã có gói đang dùng
                throw new FuncErrorException("Bạn đã có gói đăng ký đang hoạt động.");
            }
        }

        // Create subscription
        Subscription subscription = new Subscription();
        subscription.setSubscriptionCode(generateSubscriptionCode());
        subscription.setSubscriberType(subscriberType);
        subscription.setSchool(school);
        subscription.setPartnership(partnership);
        subscription.setPlan(plan);
        if (activeSubscription != null) {
            subscription.setRenewedFrom(activeSubscription);
        }
        subscription.setStartDate(LocalDateTime.now());
        subscription.setEndDate(LocalDateTime.now().plusDays(plan.getDurationDays()));

        // Free plan: activate immediately
        if (plan.getPrice().compareTo(BigDecimal.ZERO) == 0) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(subscription);
            // Nếu đang hạ từ gói trả phí về gói miễn phí: hủy gói trả phí cũ ngay lập tức
            if (activeSubscription != null && !isFreePlan(activeSubscription.getPlan())) {
                activeSubscription.setStatus(SubscriptionStatus.CANCELLED);
                activeSubscription.setCancellationReason("Hạ xuống gói miễn phí");
                activeSubscription.setCancelledAt(LocalDateTime.now());
                subscriptionRepository.save(activeSubscription);
            }
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

        return toSubscriptionResponse(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubscriptionResponse> getMySubscriptionHistory(UUID userId,
                                                                        SubscriptionStatus status,
                                                                        String keyword,
                                                                        Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        SubscriberType subscriberType = resolveSubscriberType(user);

        Specification<Subscription> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (subscriberType == SubscriberType.SCHOOL) {
                School school = schoolRepository.findByUserId(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("School profile not found."));
                predicates.add(cb.equal(root.get("school").get("id"), school.getId()));
            } else {
                Partnership partnership = partnershipRepository.findByUserId(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Partnership profile not found."));
                predicates.add(cb.equal(root.get("partnership").get("id"), partnership.getId()));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate codeLike = cb.like(cb.lower(root.get("subscriptionCode")), pattern);
                Join<Subscription, SubscriptionPlan> planJoin = root.join("plan");
                Predicate planNameLike = cb.like(cb.lower(planJoin.get("planName")), pattern);
                predicates.add(cb.or(codeLike, planNameLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Subscription> page = subscriptionRepository.findAll(spec, pageable);
        return toSubscriptionPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscriptionById(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found."));
        return toSubscriptionResponse(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubscriptionResponse> getAllSubscriptions(SubscriberType subscriberType,
                                                                   SubscriptionStatus status,
                                                                   String keyword,
                                                                   Pageable pageable) {
        Specification<Subscription> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (subscriberType != null) {
                predicates.add(cb.equal(root.get("subscriberType"), subscriberType));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate codeLike = cb.like(cb.lower(root.get("subscriptionCode")), pattern);
                Join<Subscription, SubscriptionPlan> planJoin = root.join("plan");
                Predicate planNameLike = cb.like(cb.lower(planJoin.get("planName")), pattern);
                predicates.add(cb.or(codeLike, planNameLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Subscription> page = subscriptionRepository.findAll(spec, pageable);
        return toSubscriptionPageResponse(page);
    }

    @Override
    @Transactional
    public SubscriptionResponse activatePendingSubscription(UUID subscriptionId, UUID userId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found."));

        if (subscription.getStatus() != SubscriptionStatus.PENDING_RENEWAL) {
            throw new FuncErrorException("Only pending renewal subscriptions can be activated.");
        }

        verifyOwnership(subscription, userId);

        retirePreviousActiveSubscriptionForUpgrade(subscription);

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDateTime.now());
        subscription.setEndDate(LocalDateTime.now().plusDays(subscription.getPlan().getDurationDays()));
        subscription.setCancellationReason(null);
        subscription.setCancelledAt(null);

        subscriptionRepository.save(subscription);
        log.info("Subscription {} activated manually by user {}", subscription.getSubscriptionCode(), userId);

        return toSubscriptionResponse(subscription);
    }

    @Override
    @Transactional
    public SubscriptionResponse cancelSubscription(UUID subscriptionId, String reason, UUID userId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found."));

        if (subscription.getStatus() != SubscriptionStatus.PENDING_RENEWAL) {
            throw new FuncErrorException("Only pending renewal subscriptions can be cancelled.");
        }

        verifyOwnership(subscription, userId);

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancellationReason(reason);
        subscription.setCancelledAt(LocalDateTime.now());

        subscriptionRepository.save(subscription);
        log.info("Subscription {} cancelled by user {}", subscription.getSubscriptionCode(), userId);

        return toSubscriptionResponse(subscription);
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

    private boolean isFreePlan(SubscriptionPlan plan) {
        return plan.getPrice() != null && plan.getPrice().compareTo(BigDecimal.ZERO) == 0;
    }

    // Kiểm tra ràng buộc khi trường muốn hạ xuống gói miễn phí
    private void validateDowngradeToFree(School school, SubscriptionPlan freePlan) {
        if (school == null) return;
        Integer maxStudents = freePlan.getMaxStudents();
        if (maxStudents == null) return;
        long currentStudentCount = studentRepository.countBySchoolId(school.getId());
        if (currentStudentCount > maxStudents) {
            throw new FuncErrorException(
                    "Số học sinh hiện tại (" + currentStudentCount + ") vượt quá giới hạn của gói miễn phí ("
                            + maxStudents + " học sinh). Vui lòng xóa bớt học sinh trước khi chuyển về gói miễn phí.");
        }
    }

    private void retirePreviousActiveSubscriptionForUpgrade(Subscription subscription) {
        Subscription previousSubscription = subscription.getRenewedFrom();
        if (previousSubscription == null || previousSubscription.getStatus() != SubscriptionStatus.ACTIVE) {
            return;
        }

        previousSubscription.setStatus(SubscriptionStatus.CANCELLED);
        previousSubscription.setCancellationReason("Upgraded to plan " + subscription.getPlan().getPlanName());
        previousSubscription.setCancelledAt(LocalDateTime.now());
        previousSubscription.setEndDate(LocalDateTime.now());
        subscriptionRepository.save(previousSubscription);
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

    private SubscriptionResponse toSubscriptionResponse(Subscription subscription) {
        List<SubscriptionTransactionResponse> transactions = paymentRepository
                .findAllBySubscriptionIdOrderByCreatedAtDesc(subscription.getId())
                .stream()
                .map(subscriptionMapper::toTransactionResponse)
                .toList();

        Long usedStudents = null;
        Long usedCampaignsCurrentMonth = null;
        Long usedAiQuizGenerations = null;

        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            
            if (subscription.getSubscriberType() == SubscriberType.SCHOOL && subscription.getSchool() != null) {
                usedStudents = studentRepository.countBySchoolId(subscription.getSchool().getId());
                usedCampaignsCurrentMonth = campaignRepository.countNonDraftByCreatorSchoolIdInMonth(subscription.getSchool().getId(), startOfMonth);
                usedAiQuizGenerations = aiGenerationLogRepository.countChargedBySchoolIdInPeriod(subscription.getSchool().getId(), subscription.getStartDate(), subscription.getEndDate());
            } else if (subscription.getSubscriberType() == SubscriberType.PARTNERSHIP && subscription.getPartnership() != null) {
                usedCampaignsCurrentMonth = campaignRepository.countNonDraftByCreatorPartnershipIdInMonth(subscription.getPartnership().getId(), startOfMonth);
                usedAiQuizGenerations = aiGenerationLogRepository.countChargedByPartnershipIdInPeriod(subscription.getPartnership().getId(), subscription.getStartDate(), subscription.getEndDate());
            }
        }

        return subscriptionMapper.toResponse(subscription, transactions, usedStudents, usedCampaignsCurrentMonth, usedAiQuizGenerations);
    }

    private PageResponse<SubscriptionResponse> toSubscriptionPageResponse(Page<Subscription> page) {
        List<Subscription> subscriptions = page.getContent();
        Map<UUID, List<SubscriptionTransactionResponse>> transactionMap = getTransactionMap(subscriptions);

        List<SubscriptionResponse> content = subscriptions.stream()
                .map(subscription -> {
                    List<SubscriptionTransactionResponse> txs = transactionMap.getOrDefault(subscription.getId(), Collections.emptyList());
                    return subscriptionMapper.toResponse(subscription, txs, null, null, null);
                })
                .toList();

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    private Map<UUID, List<SubscriptionTransactionResponse>> getTransactionMap(List<Subscription> subscriptions) {
        if (subscriptions.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UUID> subscriptionIds = subscriptions.stream().map(Subscription::getId).toList();
        List<Payment> payments = paymentRepository.findAllBySubscriptionIdInOrderByCreatedAtDesc(subscriptionIds);

        Map<UUID, List<SubscriptionTransactionResponse>> transactionMap = new HashMap<>();
        for (Payment payment : payments) {
            UUID subscriptionId = payment.getSubscription().getId();
            transactionMap
                    .computeIfAbsent(subscriptionId, ignored -> new ArrayList<>())
                    .add(subscriptionMapper.toTransactionResponse(payment));
        }

        return transactionMap;
    }
}
