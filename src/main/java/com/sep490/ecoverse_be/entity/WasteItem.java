package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.WasteCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "waste_items",
        indexes = {
                @Index(name = "idx_waste_items_category", columnList = "category"),
                @Index(name = "idx_waste_items_sub_category_id", columnList = "sub_category_id"),
                @Index(name = "idx_waste_items_is_active", columnList = "is_active")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WasteItem extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String itemName;

    /**
     * Top-level category — denormalized from subCategory for fast filtering.
     * Must always match subCategory.category.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WasteCategory category;

    /**
     * Sub-category this item belongs to.
     * e.g. LEAF, FRUIT, FOOD under ORGANIC.
     * Drives which items appear in a game session when School/Partnership
     * configures allowed sub-categories in RoundGameConfig.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id", nullable = false)
    private WasteSubCategory subCategory;

    @Column(columnDefinition = "text")
    private String description;

    /**
     * Fun fact shown to the student as feedback after correct classification.
     * Also used as context when AI generates quiz questions for this item.
     */
    @Column(columnDefinition = "text")
    private String funFact;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 100)
    private String decompositionTime;

    @Column(columnDefinition = "text")
    private String recyclingTips;

    @Column(length = 500)
    private String model3dUrl;

    @Column(length = 50)
    private String tripoTaskId;

    @Column(length = 20)
    private String tripoStatus;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(name = "is_delete", nullable = false)
    private boolean isDelete = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}
