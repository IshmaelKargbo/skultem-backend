package com.moriba.skultem.infrastructure.persistence.jpa;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.domain.model.AcademicYear.Status;
import com.moriba.skultem.infrastructure.persistence.entity.AcademicYearEntity;

public interface AcademicYearJpaRepository extends JpaRepository<AcademicYearEntity, String> {

    Optional<AcademicYearEntity> findFirstBySchoolIdAndStartDateAfterAndStatusNotOrderByStartDateAsc(
            String schoolId, LocalDate after, Status excludedStatus);

    Optional<AcademicYearEntity> findFirstBySchoolIdAndEndDateBeforeAndStatusNotOrderByEndDateDesc(
            String schoolId, LocalDate before, Status excludedStatus);
    boolean existsByNameIgnoreCaseAndSchoolId(String name, String school);

    boolean existsByNextYear_IdAndSchoolId(String nextYearId, String schoolId);

    Page<AcademicYearEntity> findAllBySchoolIdAndStatusNot(String schoolId, Status excludedStatus, Pageable pageable);

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
