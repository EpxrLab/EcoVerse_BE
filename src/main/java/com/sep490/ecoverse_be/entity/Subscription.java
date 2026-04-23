package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.SubscriberType;
import com.sep490.ecoverse_be.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "subscriptions", indexes = {
        @Index(name = "idx_sub_code", columnList = "subscription_code"),
        @Index(name = "idx_sub_subscriber_type", columnList = "subscriber_type"),
        @Index(name = "idx_sub_school_id", columnList = "school_id"),
        @Index(name = "idx_sub_partnership_id", columnList = "partnership_id"),
        @Index(name = "idx_sub_plan_id", columnList = "plan_id"),
        @Index(name = "idx_sub_status", columnList = "status"),
        @Index(name = "idx_sub_end_date", columnList = "end_date"),
        @Index(name = "idx_sub_school_status", columnList = "school_id, status"),
        @Index(name = "idx_sub_partnership_status", columnList = "partnership_id, status")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Subscription extends BaseEntity {

    @Column(name = "subscription_code", length = 50, unique = true, nullable = false)
    private String subscriptionCode;

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
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "start_date", nullable = false)
    private OffsetDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private OffsetDateTime endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "renewed_from_id")
    private Subscription renewedFrom;

    @Column(name = "auto_renew")
    private boolean autoRenew = false;

    @Column(name = "cancellation_reason", columnDefinition = "text")
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(columnDefinition = "text")
    private String notes;
}
