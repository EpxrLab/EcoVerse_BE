package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.CampaignType;
import com.sep490.ecoverse_be.enums.PartnershipCampaignStatus;
import com.sep490.ecoverse_be.enums.SchoolCampaignStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Entity
@Table(name = "campaigns", indexes = {
        @Index(name = "idx_campaigns_campaign_code", columnList = "campaign_code"),
        @Index(name = "idx_campaigns_campaign_type", columnList = "campaign_type"),
        @Index(name = "idx_campaigns_school_status", columnList = "school_status"),
        @Index(name = "idx_campaigns_partnership_status", columnList = "partnership_status"),
        @Index(name = "idx_campaigns_start_date", columnList = "start_date"),
        @Index(name = "idx_campaigns_end_date", columnList = "end_date"),
        @Index(name = "idx_campaigns_creator_school_id", columnList = "creator_school_id"),
        @Index(name = "idx_campaigns_creator_partnership_id", columnList = "creator_partnership_id")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Campaign extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String campaignCode;

    @Column(nullable = false, length = 255)
    private String campaignName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignType campaignType;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_school_id")
    private School creatorSchool;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_partnership_id")
    private Partnership creatorPartnership;

    @Column(nullable = false)
    private OffsetDateTime startDate;

    @Column(nullable = false)
    private OffsetDateTime endDate;

    private OffsetDateTime registrationDate;

    private OffsetDateTime registrationDeadline;

    private OffsetDateTime invitationDate;

    private OffsetDateTime invitationDeadline;

    @Enumerated(EnumType.STRING)
    private SchoolCampaignStatus schoolStatus = SchoolCampaignStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    private PartnershipCampaignStatus partnershipStatus = PartnershipCampaignStatus.DRAFT;

    private Integer maxStudentsPerSchool;

    private Integer totalStudentQuota;

    private Integer topRankingCount = 10;

    private Integer totalRounds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> rulesConfig;

    @Column(length = 500)
    private String bannerImageUrl;

    @Column(nullable = false)
    private boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}
