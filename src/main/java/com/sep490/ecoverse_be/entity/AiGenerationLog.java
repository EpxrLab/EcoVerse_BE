package com.sep490.ecoverse_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_generation_logs", indexes = {
        @Index(name = "idx_ai_gen_school_id", columnList = "school_id"),
        @Index(name = "idx_ai_gen_quiz_id", columnList = "quiz_id"),
        @Index(name = "idx_ai_gen_ai_provider", columnList = "ai_provider"),
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @Column(name = "document_url", length = 500)
    private String documentUrl;

    @Column(name = "document_type", length = 50)
    private String documentType;

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

    @Column(length = 50)
    private String status;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
