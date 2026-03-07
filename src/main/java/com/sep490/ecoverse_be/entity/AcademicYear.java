package com.sep490.ecoverse_be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "academic_years", uniqueConstraints = {
        @UniqueConstraint(name = "uk_academic_year_school_name", columnNames = {"school_id", "name"})
}, indexes = {
        @Index(name = "idx_academic_years_school_id", columnList = "school_id"),
        @Index(name = "idx_academic_years_is_active", columnList = "is_active")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AcademicYear extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
