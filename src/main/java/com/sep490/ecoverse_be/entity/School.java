package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "schools", indexes = {
        @Index(name = "idx_schools_user_id", columnList = "user_id"),
        @Index(name = "idx_schools_school_code", columnList = "school_code"),
        @Index(name = "idx_schools_approval_status", columnList = "approval_status"),
        @Index(name = "idx_schools_location", columnList = "ward, district, city")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class School extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String schoolName;

    @Column(length = 50, unique = true)
    private String schoolCode;

    @Column(columnDefinition = "text")
    private String address;

    @Column(length = 100)
    private String ward;

    @Column(length = 100)
    private String district;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String province;

    @Column(length = 100)
    private String country = "Vietnam";

    @Column(length = 20)
    private String phoneNumber;

    private String principalName;

    private String contactEmail;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime approvedAt;

    @Column(length = 500)
    private String logoUrl;

    @Column(length = 500)
    private String licenseUrl;
}
