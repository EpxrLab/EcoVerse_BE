package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.WasteCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "waste_sub_categories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_wsc_category_code",
                        columnNames = {"category", "sub_category_code"})
        },
        indexes = {
                @Index(name = "idx_wsc_category", columnList = "category"),
                @Index(name = "idx_wsc_is_active", columnList = "is_active")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WasteSubCategory extends BaseEntity {

    /**
     * Parent category this sub-category belongs to.
     * e.g. ORGANIC, RECYCLABLE, HAZARDOUS, GENERAL
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private WasteCategory category;

    /**
     * Short machine-readable code, unique within a category.
     * e.g. "LEAF", "FRUIT", "FOOD" (under ORGANIC)
     *      "PLASTIC_BOTTLE", "PAPER", "METAL" (under RECYCLABLE)
     */
    @Column(name = "sub_category_code", nullable = false, length = 50)
    private String subCategoryCode;

    /**
     * Human-readable display name shown in Admin UI and game.
     * e.g. "Lá cây", "Trái cây", "Thức ăn thừa"
     */
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "display_order")
    private int displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // ── Inverse ──────────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "subCategory", fetch = FetchType.LAZY)
    private List<WasteItem> wasteItems;
}
