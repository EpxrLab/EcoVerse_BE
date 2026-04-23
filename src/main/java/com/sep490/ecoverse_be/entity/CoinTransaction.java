package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "coin_transactions", indexes = {
        @Index(name = "idx_coin_transactions_transaction_code", columnList = "transaction_code"),
        @Index(name = "idx_coin_transactions_student_id", columnList = "student_id"),
        @Index(name = "idx_coin_transactions_transaction_type", columnList = "transaction_type"),
        @Index(name = "idx_coin_transactions_reference_id", columnList = "reference_id"),
        @Index(name = "idx_coin_transactions_campaign_id", columnList = "campaign_id"),
        @Index(name = "idx_coin_transactions_created_at", columnList = "created_at")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CoinTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 50, unique = true, nullable = false)
    private String transactionCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal balanceBefore;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal balanceAfter;

    @Column(length = 50)
    private String referenceType;

    private UUID referenceId;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
