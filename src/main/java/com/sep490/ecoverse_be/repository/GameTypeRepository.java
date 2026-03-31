package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.GameType;
import com.sep490.ecoverse_be.enums.GameTypeCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameTypeRepository extends JpaRepository<GameType, UUID> {

	boolean existsByTypeCodeAndIsActiveTrue(GameTypeCode typeCode);

	boolean existsByNameIgnoreCaseAndIsActiveTrue(String name);

	List<GameType> findByIsActiveTrue();

	Optional<GameType> findByIdAndIsActiveTrue(UUID id);

	Optional<GameType> findByTypeCode(GameTypeCode typeCode);
}



