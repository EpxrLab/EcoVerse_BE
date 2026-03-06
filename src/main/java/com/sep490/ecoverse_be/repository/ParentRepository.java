package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParentRepository extends JpaRepository<Parent, UUID> {

    Optional<Parent> findByPhoneNumber(String phoneNumber);

    Optional<Parent> findByUserId(UUID userId);

    boolean existsByPhoneNumber(String phoneNumber);
}
