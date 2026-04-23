package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.entity.Payment;
import com.sep490.ecoverse_be.entity.School;
import com.sep490.ecoverse_be.entity.Subscription;
import com.sep490.ecoverse_be.entity.SubscriptionPlan;
import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.enums.NotificationType;
import com.sep490.ecoverse_be.enums.PaymentStatus;
import com.sep490.ecoverse_be.enums.Role;
import com.sep490.ecoverse_be.enums.SubscriberType;
import com.sep490.ecoverse_be.enums.SubscriptionStatus;
import com.sep490.ecoverse_be.mapper.PaymentMapper;
import com.sep490.ecoverse_be.repository.PaymentRepository;
import com.sep490.ecoverse_be.repository.SubscriptionRepository;
import com.sep490.ecoverse_be.service.INotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.payos.PayOS;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PayOS payOS;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private INotificationService notificationService;
    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void handlePayOSWebhook_cancelsPreviousActiveSubscriptionWhenUpgradeSucceeds() {
        SubscriptionPlan freePlan = new SubscriptionPlan();
        freePlan.setPlanName("FREE");
        freePlan.setPrice(BigDecimal.ZERO);

        Subscription previousSubscription = new Subscription();
        previousSubscription.setStatus(SubscriptionStatus.ACTIVE);
        previousSubscription.setPlan(freePlan);

        SubscriptionPlan paidPlan = new SubscriptionPlan();
        paidPlan.setPlanName("PRO");
        paidPlan.setPrice(new BigDecimal("100000"));
        paidPlan.setDurationDays(30);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("school@example.com");
        user.setRole(Role.PARTNERSHIP_SCHOOL);

        School school = new School();
        school.setSchoolName("Test School");
        school.setUser(user);

        Subscription newSubscription = new Subscription();
        newSubscription.setId(UUID.randomUUID());
        newSubscription.setSubscriberType(SubscriberType.SCHOOL);
        newSubscription.setSchool(school);
        newSubscription.setStatus(SubscriptionStatus.PENDING);
        newSubscription.setPlan(paidPlan);
        newSubscription.setRenewedFrom(previousSubscription);

        Payment payment = new Payment();
        payment.setPaymentCode("PAY-TEST");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionRef("123");
        payment.setSubscription(newSubscription);

        WebhookData webhookData = org.mockito.Mockito.mock(WebhookData.class);
        when(webhookData.getOrderCode()).thenReturn(123L);
        when(webhookData.getCode()).thenReturn("00");

        when(payOS.webhooks().verify(any())).thenReturn(webhookData);
        when(paymentRepository.findByTransactionRef("123")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.handlePayOSWebhook(new Object());

        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        assertNotNull(payment.getPaidAt());

        assertEquals(SubscriptionStatus.CANCELLED, previousSubscription.getStatus());
        assertNotNull(previousSubscription.getCancelledAt());
        assertNotNull(previousSubscription.getEndDate());

        assertEquals(SubscriptionStatus.ACTIVE, newSubscription.getStatus());
        assertNotNull(newSubscription.getStartDate());
        assertNotNull(newSubscription.getEndDate());

        verify(notificationService).sendNotification(
                eq(user),
                eq(NotificationType.SYSTEM_ANNOUNCEMENT),
                eq("Subscription Activated"),
                any(String.class),
                eq("subscription"),
                eq(newSubscription.getId()),
                eq(null)
        );
    }
}

