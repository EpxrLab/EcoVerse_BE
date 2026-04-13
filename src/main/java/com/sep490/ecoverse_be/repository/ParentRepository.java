package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.Parent;
import com.sep490.ecoverse_be.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParentRepository extends JpaRepository<Parent, UUID>,
        JpaSpecificationExecutor<Parent> {

    Optional<Parent> findByPhoneNumber(String phoneNumber);

    Optional<Parent> findByUserId(UUID userId);

}
