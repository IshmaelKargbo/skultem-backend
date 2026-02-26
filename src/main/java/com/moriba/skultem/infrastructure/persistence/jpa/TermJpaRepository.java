package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.TermEntity;

@Repository
public interface TermJpaRepository extends JpaRepository<TermEntity, String> {
    boolean existsByNameIgnoreCaseAndSchoolId(String name, String school);

    @Modifying
    @Query("""
                UPDATE TermEntity a
                SET a.active = false
                WHERE a.schoolId = :schoolId
            """)
    void deactivateAllBySchoolId(@Param("schoolId") String schoolId);

    boolean existsByAcademicYearIdAndTermNumber(@Param("academicYearId") String academicYearId,
            @Param("termNumber") int termNumber);

    Optional<TermEntity> findByIdAndSchoolId(String id, String schoolId);

    Optional<TermEntity> findByIdAndAcademicYear_IdAndSchoolId(String id, String academicYearId, String schoolId);

    List<TermEntity> findAllByAcademicYearIdAndSchoolId(String academicYearId, String schoolId);

    Page<TermEntity> findAllByAcademicYearIdAndSchoolId(String academicYearId, String schoolId, Pageable pageable);

    Page<TermEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);
}
