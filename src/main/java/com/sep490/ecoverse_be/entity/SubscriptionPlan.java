package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.SubscriberType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(name = "subscription_plans", indexes = {
        @Index(name = "idx_sub_plan_code", columnList = "plan_code"),
        @Index(name = "idx_sub_plan_subscriber_type", columnList = "subscriber_type"),
        @Index(name = "idx_sub_plan_is_active", columnList = "is_active"),
        @Index(name = "idx_sub_plan_price", columnList = "price"),
        @Index(name = "idx_sub_plan_display_order", columnList = "display_order"),
        @Index(name = "idx_sub_plan_type_active", columnList = "subscriber_type, is_active")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionPlan extends BaseEntity {

    @Column(name = "plan_code", length = 50, unique = true, nullable = false)
    private String planCode;

    @Column(name = "plan_name", nullable = false)
    private String planName;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscriber_type", nullable = false)
    private SubscriberType subscriberType;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "duration_days", nullable = false)
    private int durationDays = 365;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(length = 10)
    private String currency = "VND";

    @Column(name = "max_students")
    private Integer maxStudents;

    @Column(name = "max_campaigns_per_month")
    private Integer maxCampaignsPerMonth;

    @Column(name = "max_rounds_per_campaign")
    private Integer maxRoundsPerCampaign;

    @Column(name = "max_schools_per_campaign")
    private Integer maxSchoolsPerCampaign;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> features;

    @Column(name = "grace_period_days")
    private int gracePeriodDays = 30;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "display_order")
    private int displayOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}
