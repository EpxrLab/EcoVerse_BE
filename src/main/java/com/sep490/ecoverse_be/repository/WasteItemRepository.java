package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.WasteItem;
import com.sep490.ecoverse_be.enums.WasteCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WasteItemRepository extends JpaRepository<WasteItem, UUID>, JpaSpecificationExecutor<WasteItem> {

    List<WasteItem> findByIsDeleteFalse();

    List<WasteItem> findByTripoStatus(String status);

    List<WasteItem> findBySubCategoryIdInAndIsDeleteFalseAndIsActiveTrue(List<UUID> subCategoryIds);

    List<WasteItem> findByCategoryInAndIsDeleteFalseAndIsActiveTrue(List<WasteCategory> categories);

    Optional<WasteItem> findByIdAndIsDeleteFalse(UUID id);

    boolean existsByItemNameIgnoreCaseAndSubCategoryIdAndIsDeleteFalse(String itemName, UUID subCategoryId);

    boolean existsByItemNameIgnoreCaseAndSubCategoryIdAndIdNotAndIsDeleteFalse(String itemName, UUID subCategoryId, UUID id);
}

