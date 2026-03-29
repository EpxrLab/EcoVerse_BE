package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.WasteSubCategory;
import com.sep490.ecoverse_be.enums.WasteCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WasteSubCategoryRepository extends JpaRepository<WasteSubCategory, UUID> {

    List<WasteSubCategory> findByIdInAndIsActiveTrue(List<UUID> ids);

    List<WasteSubCategory> findByIsActiveTrue();

    Optional<WasteSubCategory> findByIdAndIsActiveTrue(UUID id);

    List<WasteSubCategory> findByCategoryInAndIsActiveTrue(List<WasteCategory> categories);

    boolean existsByCategoryAndSubCategoryCode(WasteCategory category, String subCategoryCode);

    boolean existsByCategoryAndSubCategoryCodeAndIdNot(WasteCategory category, String subCategoryCode, UUID id);
}


