package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.RewardRequestStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reward_requests", indexes = {
        @Index(name = "idx_reward_requests_request_code", columnList = "request_code"),
        @Index(name = "idx_reward_requests_student_id", columnList = "student_id"),
        @Index(name = "idx_reward_requests_reward_id", columnList = "reward_id"),
        @Index(name = "idx_reward_requests_school_id", columnList = "school_id"),
        @Index(name = "idx_reward_requests_status", columnList = "status"),
        @Index(name = "idx_reward_requests_created_at", columnList = "created_at")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RewardRequest extends BaseEntity {

    @Column(length = 50, unique = true, nullable = false)
    private String requestCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_id", nullable = false)
    private Reward reward;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_parent")
    private Parent requestedByParent;

    private Integer quantity = 1;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalCoins;

    @Enumerated(EnumType.STRING)
    private RewardRequestStatus status = RewardRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    private LocalDateTime deliveredAt;

    private LocalDateTime confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private Parent confirmedByParent;

    @Column(columnDefinition = "text")
    private String rejectedReason;

    private LocalDateTime cancelledAt;

    @Column(columnDefinition = "text")
    private String cancelledReason;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(length = 500)
    private String deliveryImageUrl;
}
