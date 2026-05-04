package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.RewardStatusLog;
import com.sep490.ecoverse_be.enums.RewardLogTopic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RewardStatusLogRepository extends JpaRepository<RewardStatusLog, UUID> {

    List<RewardStatusLog> findByReferenceIdOrderByTransitionAtAsc(UUID referenceId);

    List<RewardStatusLog> findByReferenceIdAndTopicOrderByTransitionAtAsc(UUID referenceId, RewardLogTopic topic);

    Page<RewardStatusLog> findByTopicAndReferenceIdInOrderByTransitionAtDesc(
            RewardLogTopic topic, List<UUID> referenceIds, Pageable pageable);
}
