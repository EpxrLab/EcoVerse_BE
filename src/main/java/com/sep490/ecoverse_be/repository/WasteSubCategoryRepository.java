package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.WasteSubCategory;
import com.sep490.ecoverse_be.enums.WasteCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WasteSubCategoryRepository extends JpaRepository<WasteSubCategory, UUID> {

    List<WasteSubCategory> findByIdIn(List<UUID> ids);

    List<WasteSubCategory> findByIdInAndIsDeleteFalse(List<UUID> ids);

    Optional<WasteSubCategory> findByIdAndIsDeleteFalse(UUID id);

    List<WasteSubCategory> findByIsDeleteFalse();

    List<WasteSubCategory> findByCategoryInAndIsDeleteFalse(List<WasteCategory> categories);

    boolean existsByCategoryAndSubCategoryCodeAndIsDeleteFalse(WasteCategory category, String subCategoryCode);

    boolean existsByCategoryAndSubCategoryCodeAndIdNotAndIsDeleteFalse(WasteCategory category, String subCategoryCode, UUID id);
}


