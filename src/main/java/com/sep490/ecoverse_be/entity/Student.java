package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.Gender;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "students", indexes = {
        @Index(name = "idx_students_user_id", columnList = "user_id"),
        @Index(name = "idx_students_school_id", columnList = "school_id"),
        @Index(name = "idx_students_student_code", columnList = "student_code"),
        @Index(name = "idx_students_grade_level", columnList = "grade_level"),
        @Index(name = "idx_students_class_name", columnList = "class_name")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_students_school_student_code", columnNames = {"school_id", "student_code"})
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Student extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(name = "student_code", nullable = false, length = 50)
    private String studentCode;

    @Column(nullable = false)
    private String fullName;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(length = 50)
    private String gradeLevel;

    @Column(length = 100)
    private String className;

    @Column(length = 200)
    private String address;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalCoins = BigDecimal.ZERO;

    @Column(length = 500)
    private String avatarUrl;
}
