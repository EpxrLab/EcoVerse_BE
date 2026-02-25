package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.ApprovalStatus;
import com.sep490.ecoverse_be.enums.PartnershipType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "partnerships", indexes = {
        @Index(name = "idx_partnerships_user_id", columnList = "user_id"),
        @Index(name = "idx_partnerships_type", columnList = "partnership_type"),
        @Index(name = "idx_partnerships_approval_status", columnList = "approval_status"),
        @Index(name = "idx_partnerships_geo_scope", columnList = "geographic_scope_ward, geographic_scope_district")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Partnership extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String organizationName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartnershipType partnershipType;

    @Column(columnDefinition = "text")
    private String description;

    private String contactPerson;

    @Column(length = 20)
    private String phoneNumber;

    @Column(columnDefinition = "text")
    private String registeredAddress;

    @Column(length = 100)
    private String geographicScopeWard;

    @Column(length = 100)
    private String geographicScopeDistrict;

    @Column(length = 100)
    private String geographicScopeCity;

    @Column(length = 100)
    private String geographicScopeProvince;

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
