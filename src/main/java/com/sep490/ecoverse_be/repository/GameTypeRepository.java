package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.GameType;
import com.sep490.ecoverse_be.enums.GameTypeCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GameTypeRepository extends JpaRepository<GameType, UUID> {

	boolean existsByTypeCode(GameTypeCode typeCode);

	boolean existsByNameIgnoreCase(String name);

	Optional<GameType> findByTypeCode(GameTypeCode typeCode);
}



