package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.TermEntity;
import com.moriba.skultem.domain.model.Term.Status;

@Repository
public interface TermJpaRepository extends JpaRepository<TermEntity, String> {
    boolean existsByNameIgnoreCaseAndSchoolId(String name, String school);

    boolean existsByAcademicYearIdAndTermNumber(@Param("academicYearId") String academicYearId,
            @Param("termNumber") int termNumber);

    Optional<TermEntity> findByIdAndSchoolId(String id, String schoolId);

    Optional<TermEntity> findByIdAndAcademicYear_IdAndSchoolId(String id, String academicYearId, String schoolId);

    Optional<TermEntity> findFirstBySchoolIdAndStatusOrderByTermNumberAsc(String schoolId, Status status);

    Optional<TermEntity> findByTermNumberAndAcademicYear_IdAndSchoolId(int termNumber, String academicYear, String schoolId);
    
    @Query("""
        SELECT t FROM TermEntity t
        WHERE t.academicYear.id = :academicYearId
        AND t.schoolId = :schoolId
        AND t.status = 'ACTIVE'
    """)
    Optional<TermEntity> findActiveTerm(String academicYearId, String schoolId);

    List<TermEntity> findAllByAcademicYearIdAndSchoolIdOrderByTermNumberAsc(String academicYearId, String schoolId);

    Page<TermEntity> findAllByAcademicYearIdAndSchoolIdOrderByTermNumberAsc(String academicYearId, String schoolId, Pageable pageable);

    Page<TermEntity> findAllBySchoolIdOrderByTermNumberAsc(String schoolId, Pageable pageable);
}
