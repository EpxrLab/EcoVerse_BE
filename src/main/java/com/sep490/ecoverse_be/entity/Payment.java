package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.PaymentMethod;
import com.sep490.ecoverse_be.enums.PaymentStatus;
import com.sep490.ecoverse_be.enums.SubscriberType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_code", columnList = "payment_code"),
        @Index(name = "idx_payment_subscriber_type", columnList = "subscriber_type"),
        @Index(name = "idx_payment_school_id", columnList = "school_id"),
        @Index(name = "idx_payment_partnership_id", columnList = "partnership_id"),
        @Index(name = "idx_payment_subscription_id", columnList = "subscription_id"),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_method", columnList = "payment_method"),
        @Index(name = "idx_payment_transaction_ref", columnList = "transaction_ref"),
        @Index(name = "idx_payment_paid_at", columnList = "paid_at"),
        @Index(name = "idx_payment_created_at", columnList = "created_at"),
        @Index(name = "idx_payment_school_status", columnList = "school_id, status"),
        @Index(name = "idx_payment_partnership_status", columnList = "partnership_id, status")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Payment extends BaseEntity {

    @Column(name = "payment_code", length = 50, unique = true, nullable = false)
    private String paymentCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscriber_type", nullable = false)
    private SubscriberType subscriberType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partnership_id")
    private Partnership partnership;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 10)
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "transaction_ref")
    private String transactionRef;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gateway_response", columnDefinition = "jsonb")
    private Map<String, Object> gatewayResponse;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "failed_at")
    private OffsetDateTime failedAt;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @Column(name = "refunded_at")
    private OffsetDateTime refundedAt;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_reason", columnDefinition = "text")
    private String refundReason;

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(name = "invoice_url", length = 500)
    private String invoiceUrl;

    @Column(name = "payer_name")
    private String payerName;

    @Column(name = "payer_email")
    private String payerEmail;

    @Column(name = "payer_phone", length = 20)
    private String payerPhone;

    @Column(columnDefinition = "text")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}
