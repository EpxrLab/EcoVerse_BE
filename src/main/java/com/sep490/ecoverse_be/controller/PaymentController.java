package com.sep490.ecoverse_be.controller;

import com.sep490.ecoverse_be.dto.response.PaymentResponse;
import com.sep490.ecoverse_be.dto.response.ResponseDto;
import com.sep490.ecoverse_be.service.IPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Payments", description = "APIs quản lý thanh toán và tích hợp PayOS")
public class PaymentController {

    private final IPaymentService paymentService;

    /**
     * Webhook PayOS – gọi bởi PayOS khi có cập nhật trạng thái thanh toán.
     * Endpoint công khai, không yêu cầu xác thực.
     */
    @PostMapping("/webhook/payos")
    @Operation(summary = "Webhook PayOS", description = "Nhận callback từ PayOS khi trạng thái thanh toán thay đổi. Không yêu cầu xác thực.")
    public ResponseEntity<Map<String, String>> handlePayOSWebhook(@RequestBody Object body) {
        log.info("Received PayOS webhook");
        paymentService.handlePayOSWebhook(body);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    /**
     * Hủy thanh toán khi người dùng bấm hủy trên màn hình QR PayOS.
     * Frontend gọi endpoint này sau khi nhận redirect về cancelUrl.
     */
    @PostMapping("/cancel/{orderCode}")
    @Operation(
            summary = "Hủy thanh toán PayOS",
            description = """
                    Gọi endpoint này khi người dùng bấm **Hủy** trên màn hình QR PayOS và được redirect về.
                    
                    - Đánh dấu payment là `CANCELLED`.
                    - Nếu subscription đang `PENDING_RENEWAL` → hủy subscription đó.
                    - Subscription gốc đang `ACTIVE` (gói cũ) **không bị ảnh hưởng** – người dùng tiếp tục dùng gói cũ.
                    
                    Không yêu cầu xác thực (FE có thể gọi trực tiếp từ redirect).
                    """
    )
    public ResponseEntity<ResponseDto<Void>> cancelPayment(@PathVariable long orderCode) {
        paymentService.cancelPaymentByOrderCode(orderCode);
        return ResponseEntity.ok(ResponseDto.success(null, "Đã hủy thanh toán thành công."));
    }

    /**
     * Lấy trạng thái thanh toán theo order code (dùng để kiểm tra sau khi redirect).
     */
    @GetMapping("/status/{orderCode}")
    @PreAuthorize("hasAnyAuthority('PARTNERSHIP_SCHOOL', 'THIRD_PARTY_PARTNERSHIP', 'ADMINISTRATOR')")
    @Operation(summary = "Lấy trạng thái thanh toán theo order code")
    public ResponseEntity<ResponseDto<PaymentResponse>> getPaymentByOrderCode(@PathVariable long orderCode) {
        PaymentResponse response = paymentService.getPaymentByOrderCode(orderCode);
        return ResponseEntity.ok(ResponseDto.success(response, "Lấy thông tin thanh toán thành công."));
    }

    /**
     * Lấy thông tin thanh toán theo ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PARTNERSHIP_SCHOOL', 'THIRD_PARTY_PARTNERSHIP', 'ADMINISTRATOR')")
    @Operation(summary = "Lấy thông tin thanh toán theo ID")
    public ResponseEntity<ResponseDto<PaymentResponse>> getPaymentById(@PathVariable UUID id) {
        PaymentResponse response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(ResponseDto.success(response, "Lấy thông tin thanh toán thành công."));
    }
}
