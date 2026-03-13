package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.ApprovalStatus;
import com.sep490.ecoverse_be.enums.SchoolType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "schools", indexes = {
        @Index(name = "idx_schools_user_id", columnList = "user_id"),
        @Index(name = "idx_schools_tax_code", columnList = "tax_code"),
        @Index(name = "idx_schools_approval_status", columnList = "approval_status"),
        @Index(name = "idx_schools_location", columnList = "ward")
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SchoolType schoolType;

    //đổi lại là mã số thuế
    @Column(length = 50, unique = true)
    private String taxCode;

    @Column(columnDefinition = "text")
    private String address;

    @Column(length = 100)
    private String ward;

    @Column(length = 100)
    private String province;

    @Column(length = 100)
    private String country = "Vietnam";

    //sửa lại thành 10
    @Column(length = 10)
    private String phoneNumber;

    private String principalName;

    private String contactEmail;

    //thêm chức vụ
    @Column(length = 100)
    private String position;

    //thêm link web
    @Column(length = 100)
    private String linkWeb;

    @Column(columnDefinition = "text")
    private String description;

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
