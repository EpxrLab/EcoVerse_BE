package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.ParticipationStatus;
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
@Table(name = "campaign_schools_participate",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_campaign_schools_participate_campaign_school", columnNames = {"campaign_id", "school_id"})
        },
        indexes = {
                @Index(name = "idx_campaign_schools_participate_campaign_id", columnList = "campaign_id"),
                @Index(name = "idx_campaign_schools_participate_school_id", columnList = "school_id"),
                @Index(name = "idx_campaign_schools_participate_status", columnList = "status")
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CampaignSchoolParticipate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(nullable = false)
    private int studentsEnrolled = 0;

    /** Số học sinh tối đa được mời cho trường này (partnership campaign). */
    private Integer maxStudentsInvited;

    private OffsetDateTime invitationSentAt;

    private OffsetDateTime participationConfirmedAt;

    @Enumerated(EnumType.STRING)
    private ParticipationStatus status = ParticipationStatus.PREPARED;

    @Column(columnDefinition = "text")
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
