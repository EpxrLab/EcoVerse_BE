package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.ParticipationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_participants",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_campaign_participants_campaign_student", columnNames = {"campaign_id", "student_id"})
        },
        indexes = {
                @Index(name = "idx_campaign_participants_campaign_id", columnList = "campaign_id"),
                @Index(name = "idx_campaign_participants_student_id", columnList = "student_id"),
                @Index(name = "idx_campaign_participants_school_id", columnList = "school_id"),
                @Index(name = "idx_campaign_participants_parent_approval_status", columnList = "parent_approval_status")
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CampaignParticipant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    private LocalDateTime enrollmentDate;

    @Enumerated(EnumType.STRING)
        private ParticipationStatus parentApprovalStatus = ParticipationStatus.PENDING_PARENT_APPROVAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_approved_by")
    private Parent parentApprovedBy;

    private LocalDateTime parentApprovedAt;

    @Column(nullable = false)
    private boolean isActive = true;
}
