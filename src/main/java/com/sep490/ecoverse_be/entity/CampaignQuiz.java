package com.sep490.ecoverse_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "campaign_quizzes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_campaign_quizzes_campaign_quiz", columnNames = {"campaign_id", "quiz_id"})
        },
        indexes = {
                @Index(name = "idx_campaign_quizzes_campaign_id", columnList = "campaign_id"),
                @Index(name = "idx_campaign_quizzes_quiz_id", columnList = "quiz_id")
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CampaignQuiz {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> unlockCondition;

    @Column(nullable = false)
    private boolean isRequired = false;

    private Integer displayOrder;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
