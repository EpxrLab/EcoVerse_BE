package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID>,
        JpaSpecificationExecutor<Student> {

    List<Student> findBySchoolId(UUID schoolId);

    Optional<Student> findByUserId(UUID userId);

    long countBySchoolId(UUID schoolId);

    @Query("SELECT s.studentCode FROM Student s WHERE s.school.id = :schoolId AND s.studentCode LIKE :prefix%")
    List<String> findStudentCodesBySchoolIdAndPrefix(@Param("schoolId") UUID schoolId, @Param("prefix") String prefix);

    boolean existsBySchoolIdAndStudentCode(UUID schoolId, String studentCode);

    // ── Report aggregate queries ───────────────────────────────────────────────

    @Query("SELECT s FROM Student s WHERE s.school.id = :schoolId ORDER BY s.totalCoins DESC")
    List<Student> findTopBySchoolIdOrderByTotalCoinsDesc(@Param("schoolId") UUID schoolId, org.springframework.data.domain.Pageable pageable);
}
