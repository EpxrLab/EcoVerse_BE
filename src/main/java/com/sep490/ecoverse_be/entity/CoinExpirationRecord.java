package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.CoinExpirationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coin_expiration_records", indexes = {
        @Index(name = "idx_coin_exp_student_id", columnList = "student_id"),
        @Index(name = "idx_coin_exp_school_id", columnList = "school_id"),
        @Index(name = "idx_coin_exp_subscription_id", columnList = "subscription_id"),
        @Index(name = "idx_coin_exp_status", columnList = "status"),
        @Index(name = "idx_coin_exp_scheduled_date", columnList = "scheduled_date"),
        @Index(name = "idx_coin_exp_school_status", columnList = "school_id, status")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CoinExpirationRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CoinExpirationStatus status = CoinExpirationStatus.SCHEDULED;

    @Column(name = "coins_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal coinsAmount;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDateTime scheduledDate;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private CoinTransaction transaction;
}
