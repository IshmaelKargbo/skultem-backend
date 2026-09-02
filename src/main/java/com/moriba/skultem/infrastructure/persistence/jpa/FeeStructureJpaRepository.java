package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.infrastructure.persistence.entity.FeeStructureEntity;

public interface FeeStructureJpaRepository extends JpaRepository<FeeStructureEntity, String> {
    boolean existsByAcademicYear_IdAndClazz_IdAndTerm_IdAndCategory_IdAndSchoolId(String academicYearId, String classId,
            String termId, String categoryId, String schoolId);

    boolean existsByCategory_IdAndSchoolId(String categoryId, String schoolId);

    boolean existsBySystemTrueAndAcademicYear_IdAndSchoolId(String academicYearId, String schoolId);

    Optional<FeeStructureEntity> findBySystemTrueAndAcademicYear_IdAndSchoolId(String academicYearId,
            String schoolId);

    // SELECTION is deliberately excluded - it's an explicit, one-off assignment to specific
    // students made once at creation (see CreateFeeStructureUseCase), never something a later
    // enrollment (new admission or promotion) should auto-pick-up just because it also has a null
    // clazz. Without this, a fee meant for a handful of hand-picked students would silently reach
    // every future enrollment in every class.
    @Query("""
                SELECT f
                FROM FeeStructureEntity f
                WHERE f.schoolId = :schoolId
                AND f.academicYear.id = :academicYearId
                AND f.type <> 'SELECTION'
                AND (
                        f.clazz.id = :classId
                     OR f.clazz IS NULL
                )
            """)
    List<FeeStructureEntity> findApplicableFees(
            @Param("schoolId") String schoolId,
            @Param("academicYearId") String academicYearId,
            @Param("classId") String classId);

    Page<FeeStructureEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    Optional<FeeStructureEntity> findByIdAndSchoolId(String id, String schoolId);

    Page<FeeStructureEntity> findAllByClazz_IdAndSchoolIdOrderByCreatedAtDesc(String classId, String schoolId,
            Pageable pageable);

    Page<FeeStructureEntity> findAllByAcademicYear_IdAndSchoolIdOrderByCreatedAtDesc(String academic, String schoolId,
            Pageable pageable);

    Page<FeeStructureEntity> findAllByAcademicYear_IdAndClazz_IdAndSchoolIdOrderByCreatedAtDesc(String academicId,
            String classId, String schoolId, Pageable pageable);

    Page<FeeStructureEntity> findAllByTerm_IdAndSchoolIdOrderByCreatedAtDesc(String termId, String schoolId,
            Pageable pageable);

    @Query("""
                SELECT f FROM FeeStructureEntity f
                WHERE f.schoolId = :schoolId
                AND (:termId IS NULL OR f.term.id = :termId)
                ORDER BY f.createdAt DESC
            """)
    Page<FeeStructureEntity> search(
            @Param("schoolId") String schoolId,
            @Param("termId") String termId,
            Pageable pageable);
}
