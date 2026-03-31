package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.WasteItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WasteItemRepository extends JpaRepository<WasteItem, UUID>, JpaSpecificationExecutor<WasteItem> {

    List<WasteItem> findByIsDeleteFalse();

    Optional<WasteItem> findByIdAndIsDeleteFalse(UUID id);

    boolean existsByItemNameIgnoreCaseAndSubCategoryIdAndIsDeleteFalse(String itemName, UUID subCategoryId);

    boolean existsByItemNameIgnoreCaseAndSubCategoryIdAndIdNotAndIsDeleteFalse(String itemName, UUID subCategoryId, UUID id);
}

