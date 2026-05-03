package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.StudentParentLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentParentLinkRepository extends JpaRepository<StudentParentLink, UUID> {

    List<StudentParentLink> findByParentId(UUID parentId);

    List<StudentParentLink> findByStudentId(UUID studentId);

    Optional<StudentParentLink> findFirstByStudentId(UUID studentId);

    Optional<StudentParentLink> findFirstByParentId(UUID parentId);

    boolean existsByStudentIdAndParentId(UUID studentId, UUID parentId);

    void deleteByStudentId(UUID studentId);
}
