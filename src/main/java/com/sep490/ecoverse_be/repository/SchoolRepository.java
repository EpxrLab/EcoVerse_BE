package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SchoolRepository extends JpaRepository<School, Long> {

    Optional<School> findByUserId(Long userId);
}
