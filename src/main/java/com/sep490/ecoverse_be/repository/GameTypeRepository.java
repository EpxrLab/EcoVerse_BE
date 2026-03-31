package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.GameType;
import com.sep490.ecoverse_be.enums.GameTypeCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameTypeRepository extends JpaRepository<GameType, UUID> {

	boolean existsByTypeCodeAndIsDeleteFalse(GameTypeCode typeCode);

	boolean existsByNameIgnoreCaseAndIsDeleteFalse(String name);

	List<GameType> findByIsDeleteFalse();

	Optional<GameType> findByIdAndIsDeleteFalse(UUID id);

	Optional<GameType> findByTypeCode(GameTypeCode typeCode);
}



