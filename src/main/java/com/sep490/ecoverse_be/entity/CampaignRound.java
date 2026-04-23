package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.RoundStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Entity
@Table(name = "campaign_rounds",
        uniqueConstraints = @UniqueConstraint(columnNames = {"campaign_id", "round_number"}),
        indexes = {
                @Index(name = "idx_campaign_rounds_campaign_id", columnList = "campaign_id"),
                @Index(name = "idx_campaign_rounds_status", columnList = "status"),
                @Index(name = "idx_campaign_rounds_start_time", columnList = "start_time")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CampaignRound extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Column(name = "round_name", length = 255, nullable = false)
    private String roundName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RoundStatus status = RoundStatus.UPCOMING;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "advance_count")
    private Integer advanceCount;

    @Column(name = "advance_criteria", length = 100)
    private String advanceCriteria = "BEST_ACCURACY_THEN_TIME";

    @Column(name = "advance_threshold", precision = 5, scale = 2)
    private BigDecimal advanceThreshold;

    @Column(name = "is_final_round")
    private Boolean isFinalRound = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "campaign_round_quizzes",
            joinColumns = @JoinColumn(name = "campaign_round_id"),
            inverseJoinColumns = @JoinColumn(name = "quiz_id")
    )
    private List<Quiz> selectedQuizzes;
}
