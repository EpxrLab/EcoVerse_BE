package com.sep490.ecoverse_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * [NEW] School-scoped coin wallet per student.
 * Extracted from Student.totalCoins to allow proper locking during concurrent
 * earn/spend operations and to support future multi-school scenarios cleanly.
 *
 * School-only: Partnership campaigns have no coin economy.
 */
@Entity
@Table(name = "student_wallets",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sw_student_school",
                        columnNames = {"student_id", "school_id"})
        },
        indexes = {
                @Index(name = "idx_sw_student_id", columnList = "student_id"),
                @Index(name = "idx_sw_school_id", columnList = "school_id")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StudentWallet extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(name = "coin_balance", nullable = false)
    private int coinBalance = 0;

    @Column(name = "coin_spent", nullable = false)
    private int coinSpent = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}