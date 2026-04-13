package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.TitleCriteriaType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "campaign_titles", indexes = {
        @Index(name = "idx_camp_title_campaign_id", columnList = "campaign_id"),
        @Index(name = "idx_camp_title_criteria_type", columnList = "criteria_type"),
        @Index(name = "idx_camp_title_is_active", columnList = "is_active"),
        @Index(name = "idx_camp_title_school_id", columnList = "school_id"),
        @Index(name = "idx_camp_title_partnership_id", columnList = "partnership_id")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CampaignTitle extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partnership_id")
    private Partnership partnership;

    @Column(name = "title_name", nullable = false)
    private String titleName;

    @Column(name = "display_format", length = 500, nullable = false)
    private String displayFormat;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "criteria_type", nullable = false)
    private TitleCriteriaType criteriaType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "criteria_config", columnDefinition = "jsonb")
    private Map<String, Object> criteriaConfig;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "max_recipients")
    private int maxRecipients = 1;

    @Column(name = "is_active")
    private boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}
