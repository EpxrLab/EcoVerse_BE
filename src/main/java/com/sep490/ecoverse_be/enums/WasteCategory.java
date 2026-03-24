package com.sep490.ecoverse_be.enums;

/**
 * Top-level waste categories. These are fixed and managed by Admin.
 * Each category can have multiple WasteSubCategory entries (e.g. ORGANIC → LEAF, FRUIT, FOOD).
 * School/Partnership cannot create new top-level categories;
 * they can only select which sub-categories appear in their game rounds.
 */
public enum WasteCategory {
    RECYCLABLE,
    ORGANIC,
    HAZARDOUS,
    GENERAL
}
