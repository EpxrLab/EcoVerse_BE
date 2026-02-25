package com.sep490.ecoverse_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_parent_links", uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_parent", columnNames = {"student_id", "parent_id"})
}, indexes = {
        @Index(name = "idx_spl_student_id", columnList = "student_id"),
        @Index(name = "idx_spl_parent_id", columnList = "parent_id")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StudentParentLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    @Column(length = 50)
    private String relationship;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
