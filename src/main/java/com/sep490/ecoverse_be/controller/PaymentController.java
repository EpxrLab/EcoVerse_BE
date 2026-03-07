package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.response.PaymentResponse;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.service.IPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final IPaymentService paymentService;

    /**
     * PayOS webhook endpoint.
     * This endpoint is called by PayOS to notify about payment status changes.
     * Must be publicly accessible (no authentication required).
     */
    @PostMapping("/webhook/payos")
    public ResponseEntity<Map<String, String>> handlePayOSWebhook(@RequestBody Object body) {
        log.info("Received PayOS webhook");
        paymentService.handlePayOSWebhook(body);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    /**
     * Get payment status by order code (for frontend to check after redirect).
     */
    @GetMapping("/status/{orderCode}")
    @PreAuthorize("hasAnyRole('PARTNERSHIP_SCHOOL', 'THIRD_PARTY_PARTNERSHIP', 'ADMINISTRATOR')")
    public ResponseEntity<ResponseDto<PaymentResponse>> getPaymentByOrderCode(@PathVariable long orderCode) {
        PaymentResponse response = paymentService.getPaymentByOrderCode(orderCode);
        return ResponseEntity.ok(ResponseDto.success(response, "Payment status retrieved."));
    }

    /**
     * Get payment by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PARTNERSHIP_SCHOOL', 'THIRD_PARTY_PARTNERSHIP', 'ADMINISTRATOR')")
    public ResponseEntity<ResponseDto<PaymentResponse>> getPaymentById(@PathVariable UUID id) {
        PaymentResponse response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(ResponseDto.success(response, "Payment retrieved."));
    }
}
