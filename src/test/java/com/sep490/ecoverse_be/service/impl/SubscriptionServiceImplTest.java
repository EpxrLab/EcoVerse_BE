package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.CreateSubscriptionRequest;
import com.sep490.ecoverse_be.dto.response.PaymentResponse;
import com.sep490.ecoverse_be.entity.School;
import com.sep490.ecoverse_be.entity.Subscription;
import com.sep490.ecoverse_be.entity.SubscriptionPlan;
import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.enums.Role;
import com.sep490.ecoverse_be.enums.SubscriberType;
import com.sep490.ecoverse_be.enums.SubscriptionStatus;
import com.sep490.ecoverse_be.exception.FuncErrorException;
import com.sep490.ecoverse_be.mapper.PaymentMapper;
import com.sep490.ecoverse_be.mapper.SubscriptionMapper;
import com.sep490.ecoverse_be.repository.PartnershipRepository;
import com.sep490.ecoverse_be.repository.PaymentRepository;
import com.sep490.ecoverse_be.repository.SchoolRepository;
import com.sep490.ecoverse_be.repository.SubscriptionPlanRepository;
import com.sep490.ecoverse_be.repository.SubscriptionRepository;
import com.sep490.ecoverse_be.repository.UserRepository;
import com.sep490.ecoverse_be.service.IPaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;
    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private PartnershipRepository partnershipRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private IPaymentService paymentService;
    @Mock
    private SubscriptionMapper subscriptionMapper;
    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    @Test
    void subscribe_allowsUpgradeFromActiveFreeToPaid() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail("school@example.com");
        user.setRole(Role.PARTNERSHIP_SCHOOL);

        School school = new School();
        school.setId(UUID.randomUUID());
        school.setUser(user);

        SubscriptionPlan freePlan = buildPlan("FREE", BigDecimal.ZERO, SubscriberType.SCHOOL);
        Subscription activeFree = new Subscription();
        activeFree.setStatus(SubscriptionStatus.ACTIVE);
        activeFree.setPlan(freePlan);

        SubscriptionPlan paidPlan = buildPlan("PRO", new BigDecimal("100000"), SubscriberType.SCHOOL);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(paidPlan));
        when(schoolRepository.findByUserId(userId)).thenReturn(Optional.of(school));
        when(subscriptionRepository.findBySchoolIdAndStatus(school.getId(), SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(activeFree));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse expectedResponse = new PaymentResponse(
                UUID.randomUUID(),
                "PAY-TEST",
                SubscriberType.SCHOOL,
                "Test School",
                UUID.randomUUID(),
                "SUB-TEST",
                paidPlan.getPrice(),
                "VND",
                null,
                null,
                null,
                null,
                null,
                null,
                user.getEmail(),
                null,
                "https://checkout.example",
                null
        );
        when(paymentService.createPayment(any(Subscription.class), eq(user))).thenReturn(expectedResponse);

        PaymentResponse result = subscriptionService.subscribe(new CreateSubscriptionRequest(planId), userId);

        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(paymentService).createPayment(subscriptionCaptor.capture(), eq(user));

        Subscription createdSubscription = subscriptionCaptor.getValue();
        assertEquals(SubscriptionStatus.PENDING_RENEWAL, createdSubscription.getStatus());
        assertSame(activeFree, createdSubscription.getRenewedFrom());
        assertNotNull(createdSubscription.getSubscriptionCode());
        assertSame(expectedResponse, result);
    }

    @Test
    void subscribe_blocksWhenActivePaidSubscriptionExists() {
        UUID userId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setRole(Role.PARTNERSHIP_SCHOOL);

        School school = new School();
        school.setId(UUID.randomUUID());
        school.setUser(user);

        SubscriptionPlan activePaidPlan = buildPlan("PRO", new BigDecimal("100000"), SubscriberType.SCHOOL);
        Subscription activePaid = new Subscription();
        activePaid.setStatus(SubscriptionStatus.ACTIVE);
        activePaid.setPlan(activePaidPlan);

        SubscriptionPlan requestedPlan = buildPlan("PREMIUM", new BigDecimal("200000"), SubscriberType.SCHOOL);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subscriptionPlanRepository.findById(planId)).thenReturn(Optional.of(requestedPlan));
        when(schoolRepository.findByUserId(userId)).thenReturn(Optional.of(school));
        when(subscriptionRepository.findBySchoolIdAndStatus(school.getId(), SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(activePaid));

        assertThrows(FuncErrorException.class,
                () -> subscriptionService.subscribe(new CreateSubscriptionRequest(planId), userId));
    }

    private SubscriptionPlan buildPlan(String code, BigDecimal price, SubscriberType subscriberType) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setPlanCode(code);
        plan.setPlanName(code + " Plan");
        plan.setPrice(price);
        plan.setCurrency("VND");
        plan.setDurationDays(30);
        plan.setSubscriberType(subscriberType);
        plan.setActive(true);
        return plan;
    }
}

