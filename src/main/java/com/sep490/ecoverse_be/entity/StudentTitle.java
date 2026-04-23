package com.sep490.ecoverse_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "student_titles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "campaign_title_id"}),
        indexes = {
                @Index(name = "idx_stu_title_student_id", columnList = "student_id"),
                @Index(name = "idx_stu_title_campaign_title_id", columnList = "campaign_title_id"),
                @Index(name = "idx_stu_title_campaign_id", columnList = "campaign_id"),
                @Index(name = "idx_stu_title_earned_at", columnList = "earned_at"),
                @Index(name = "idx_stu_title_is_displayed", columnList = "is_displayed")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StudentTitle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_title_id", nullable = false)
    private CampaignTitle campaignTitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(name = "display_text", length = 500, nullable = false)
    private String displayText;

    @CreationTimestamp
    @Column(name = "earned_at", updatable = false)
    private OffsetDateTime earnedAt;

    @Column(name = "metric_value", length = 100)
    private String metricValue;

    @Column(name = "is_displayed")
    private boolean isDisplayed = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
