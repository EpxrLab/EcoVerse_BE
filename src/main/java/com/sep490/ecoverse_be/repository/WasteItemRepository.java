package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.WasteItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WasteItemRepository extends JpaRepository<WasteItem, UUID>, JpaSpecificationExecutor<WasteItem> {

    List<WasteItem> findByIsActiveTrue();

    Optional<WasteItem> findByIdAndIsActiveTrue(UUID id);

    boolean existsByItemNameIgnoreCaseAndSubCategoryId(String itemName, UUID subCategoryId);

    boolean existsByItemNameIgnoreCaseAndSubCategoryIdAndIdNot(String itemName, UUID subCategoryId, UUID id);
}

