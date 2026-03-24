package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.Reward;
import com.sep490.ecoverse_be.enums.RewardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RewardRepository extends JpaRepository<Reward, UUID> {

    List<Reward> findBySchoolIdAndIsActiveTrueOrderByCreatedAtDesc(UUID schoolId);

    List<Reward> findBySchoolIdAndRewardTypeAndIsActiveTrueOrderByCreatedAtDesc(UUID schoolId, RewardType rewardType);

    Optional<Reward> findByIdAndSchoolIdAndIsActiveTrue(UUID id, UUID schoolId);

    boolean existsByRewardNameAndSchoolIdAndIsActiveTrue(String rewardName, UUID schoolId);
}
