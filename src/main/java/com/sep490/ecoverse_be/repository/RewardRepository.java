package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.Reward;
import com.sep490.ecoverse_be.enums.RewardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RewardRepository extends JpaRepository<Reward, UUID> {

    List<Reward> findBySchoolIdAndIsDeleteFalseOrderByCreatedAtDesc(UUID schoolId);

    List<Reward> findBySchoolIdAndRewardTypeAndIsDeleteFalseOrderByCreatedAtDesc(UUID schoolId, RewardType rewardType);

    Optional<Reward> findByIdAndSchoolIdAndIsDeleteFalse(UUID id, UUID schoolId);

    boolean existsByRewardNameAndSchoolIdAndIsDeleteFalse(String rewardName, UUID schoolId);
}
