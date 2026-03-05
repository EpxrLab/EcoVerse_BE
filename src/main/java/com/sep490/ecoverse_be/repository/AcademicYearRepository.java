package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {

    Optional<AcademicYear> findBySchoolIdAndName(UUID schoolId, String name);

    List<AcademicYear> findBySchoolId(UUID schoolId);
}
