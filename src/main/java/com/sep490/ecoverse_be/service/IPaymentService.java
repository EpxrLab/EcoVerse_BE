package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.response.PaymentResponse;
import com.sep490.ecoverse_be.entity.Subscription;
import com.sep490.ecoverse_be.entity.User;

import java.util.UUID;

public interface IPaymentService {

    // Create a PayOS payment link for a subscription
    PaymentResponse createPayment(Subscription subscription, User user);

    // Handle PayOS webhook callback
    void handlePayOSWebhook(Object webhookBody);

    // Get payment by ID
    PaymentResponse getPaymentById(UUID paymentId);

    // Get payment by order code (for PayOS return URL handling)
    PaymentResponse getPaymentByOrderCode(long orderCode);
}
