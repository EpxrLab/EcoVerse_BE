package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "data_deletion_requests", indexes = {
        @Index(name = "idx_del_req_requested_by", columnList = "requested_by_user_id"),
        @Index(name = "idx_del_req_student_id", columnList = "student_id"),
        @Index(name = "idx_del_req_status", columnList = "status"),
        @Index(name = "idx_del_req_created_at", columnList = "created_at")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DataDeletionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(name = "request_reason", columnDefinition = "text")
    private String requestReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedByUser;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "deletion_metadata", columnDefinition = "jsonb")
    private Map<String, Object> deletionMetadata;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
