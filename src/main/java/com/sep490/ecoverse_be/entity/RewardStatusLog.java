package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.RewardLogTopic;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reward_status_logs", indexes = {
        @Index(name = "idx_rsl_reference_id", columnList = "reference_id"),
        @Index(name = "idx_rsl_topic", columnList = "topic"),
        @Index(name = "idx_rsl_transition_at", columnList = "transition_at"),
        @Index(name = "idx_rsl_actor_user_id", columnList = "actor_user_id"),
        @Index(name = "idx_rsl_topic_reference", columnList = "topic, reference_id")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RewardStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "topic", nullable = false, length = 30)
    private RewardLogTopic topic;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 30)
    private String toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actorUser;

    @Column(name = "actor_name", length = 255)
    private String actorName;

    @Column(name = "actor_role", length = 50)
    private String actorRole;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "transition_at", nullable = false)
    private OffsetDateTime transitionAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
