package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.GameSessionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GameSessionItemRepository extends JpaRepository<GameSessionItem, UUID> {

    List<GameSessionItem> findByGameSessionIdOrderByLevelNumberAscPresentedOrderAsc(UUID gameSessionId);

    void deleteByGameSessionId(UUID gameSessionId);
}
