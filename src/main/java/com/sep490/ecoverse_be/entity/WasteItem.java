package com.sep490.ecoverse_be.entity;

import com.sep490.ecoverse_be.enums.WasteCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "waste_items", indexes = {
        @Index(name = "idx_waste_items_category", columnList = "category"),
        @Index(name = "idx_waste_items_is_active", columnList = "is_active")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WasteItem extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WasteCategory category;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String funFact;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 100)
    private String decompositionTime;

    @Column(columnDefinition = "text")
    private String recyclingTips;

    @Column(nullable = false)
    private boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}
