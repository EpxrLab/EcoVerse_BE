package com.sep490.ecoverse_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ai_generation_logs",
        indexes = {
                @Index(name = "idx_ai_gen_school_id", columnList = "school_id"),
                @Index(name = "idx_ai_gen_partnership_id", columnList = "partnership_id"),
                @Index(name = "idx_ai_gen_quiz_id", columnList = "quiz_id"),
                @Index(name = "idx_ai_gen_round_game_config_id", columnList = "round_game_config_id"),
                @Index(name = "idx_ai_gen_created_by", columnList = "created_by"),
                @Index(name = "idx_ai_gen_is_usage_charged", columnList = "is_usage_charged"),
                @Index(name = "idx_ai_gen_created_at", columnList = "created_at"),
                @Index(name = "idx_ai_gen_status", columnList = "status")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AiGenerationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ── Owner ─────────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partnership_id")
    private Partnership partnership;

    // ── Context used for generation ───────────────────────────────────────────

    /**
     * The RoundGameConfig whose game + sub-categories provided the waste item pool
     * used as AI context. Captured at generation time so we know exactly which items
     * fed into the prompt.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_game_config_id")
    private RoundGameConfig roundGameConfig;

    /**
     * Snapshot of the waste item IDs used as context for this generation.
     * Stored so the prompt can be reproduced / audited even if items change later.
     * Format: { "wasteItemIds": ["uuid1", "uuid2", ...], "subCategoryCodes": [...] }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "waste_item_context_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> wasteItemContextSnapshot;

    /**
     * Grade level provided by School/Partnership in the wizard.
     */
    @Column(name = "target_grade")
    private Integer targetGrade;

    /**
     * Number of questions requested.
     */
    @Column(name = "requested_question_count")
    private Integer requestedQuestionCount;

    /**
     * URL of supplementary document uploaded by School/Partnership (if any).
     */
    @Column(name = "document_url", length = 500)
    private String documentUrl;

    @Column(name = "document_type", length = 50)
    private String documentType;

    // ── AI call details ───────────────────────────────────────────────────────

    @Column(name = "ai_provider", length = 50)
    private String aiProvider;

    @Column(name = "ai_model", length = 100)
    private String aiModel;

    @Column(name = "prompt_used", columnDefinition = "text")
    private String promptUsed;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "generation_duration_ms")
    private Integer generationDurationMs;

    @Column(name = "cost_usd", precision = 10, scale = 4)
    private BigDecimal costUsd;

    @Column(name = "questions_generated")
    private Integer questionsGenerated;

    /** SUCCESS, FAILED, TIMEOUT */
    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    // ── Result ────────────────────────────────────────────────────────────────

    /**
     * The quiz created from this generation attempt.
     * NULL if generation failed.
     * Set to isPublished=false initially (preview state).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    // ── Usage quota tracking ──────────────────────────────────────────────────

    /**
     * Whether this generation has been counted against the subscription quota.
     *
     * false = School/Partnership has not yet confirmed the preview quiz.
     *         (They can discard without consuming a quota slot.)
     * true  = School/Partnership confirmed the quiz (isPublished flipped to true).
     *         One AI quiz generation quota unit has been consumed.
     *
     * This separates "AI was called" from "quota was consumed":
     * failed generations and discarded previews do NOT count toward the quota.
     */
    @Column(name = "is_usage_charged", nullable = false)
    private boolean isUsageCharged = false;

    @Column(name = "usage_charged_at")
    private OffsetDateTime usageChargedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
