package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.AcademicYearEntity;

@Repository
public interface AcademicYearJpaRepository extends JpaRepository<AcademicYearEntity, String> {
    boolean existsByNameIgnoreCaseAndSchoolId(String name, String school);

    @Modifying
    @Query("""
                UPDATE AcademicYearEntity a
                SET a.active = false
                WHERE a.schoolId = :schoolId
            """)
    void deactivateAllBySchoolId(@Param("schoolId") String schoolId);

    Optional<AcademicYearEntity> findBySchoolIdAndActiveTrue(String schoolId);

    Optional<AcademicYearEntity> findByIdAndSchoolId(String id, String schoolId);

    Page<AcademicYearEntity> findAllBySchoolId(String schoolId, Pageable pageable);
}
