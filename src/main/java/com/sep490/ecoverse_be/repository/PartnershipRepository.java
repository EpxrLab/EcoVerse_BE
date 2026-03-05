package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.Partnership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartnershipRepository extends JpaRepository<Partnership, Long> {

    Optional<Partnership> findByUserId(Long userId);
}
