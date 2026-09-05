package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.domain.vo.Gender;
import com.moriba.skultem.infrastructure.persistence.entity.FeeStructureEntity;

public interface FeeStructureJpaRepository extends JpaRepository<FeeStructureEntity, String> {
    // A fee structure only "overlaps" (and should be blocked as a duplicate) another one covering
    // the same class/term/category/year if the two could ever charge the same student twice. A
    // plain fee (neither flag set, no gender) overlaps everything, since it reaches every student
    // including whichever slice a targeted fee reaches. newStudentsOnly and oldStudentsOnly never
    // overlap each other - they're a deliberate partition (e.g. Tuition: 900 for new students, 700
    // for old/returning students in the same class/term) - see CreateFeeStructureUseCase. Gender
    // works the same way: a MALE-only and a FEMALE-only fee (e.g. two Uniform fees priced
    // differently per gender) never overlap each other, but either overlaps a fee with no gender
    // restriction, since that one still reaches their students too.
    @Query("""
                SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END
                FROM FeeStructureEntity f
                WHERE f.schoolId = :schoolId
                AND f.academicYear.id = :academicYearId
                AND f.term.id = :termId
                AND f.clazz.id = :classId
                AND f.category.id = :categoryId
                AND (
                        (:newStudentsOnly = true AND f.oldStudentsOnly = false)
                     OR (:oldStudentsOnly = true AND f.newStudentsOnly = false)
                     OR (:newStudentsOnly = false AND :oldStudentsOnly = false)
                )
                AND (:gender IS NULL OR f.gender IS NULL OR f.gender = :gender)
            """)
    boolean existsOverlappingFeeStructure(
            @Param("schoolId") String schoolId,
            @Param("academicYearId") String academicYearId,
            @Param("termId") String termId,
            @Param("classId") String classId,
            @Param("categoryId") String categoryId,
            @Param("newStudentsOnly") boolean newStudentsOnly,
            @Param("oldStudentsOnly") boolean oldStudentsOnly,
            @Param("gender") Gender gender);

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

    // classId/newStudentsOnly/oldStudentsOnly are all optional filters (a school narrowing the list
    // down to, say, "Class 1's old-students Tuition fee") - each is skipped when left null, same
    // pattern as the existing termId filter. No static ORDER BY here - the caller's Pageable always
    // carries an explicit Sort (see ListFeeStructureBySchoolUseCase), which Spring Data appends for
    // us; a query can't have both a literal ORDER BY and an injected Pageable sort.
    @Query("""
                SELECT f FROM FeeStructureEntity f
                WHERE f.schoolId = :schoolId
                AND (:termId IS NULL OR f.term.id = :termId)
                AND (:classId IS NULL OR f.clazz.id = :classId)
                AND (:newStudentsOnly IS NULL OR f.newStudentsOnly = :newStudentsOnly)
                AND (:oldStudentsOnly IS NULL OR f.oldStudentsOnly = :oldStudentsOnly)
                AND (:gender IS NULL OR f.gender = :gender)
            """)
    Page<FeeStructureEntity> search(
            @Param("schoolId") String schoolId,
            @Param("termId") String termId,
            @Param("classId") String classId,
            @Param("newStudentsOnly") Boolean newStudentsOnly,
            @Param("oldStudentsOnly") Boolean oldStudentsOnly,
            @Param("gender") Gender gender,
            Pageable pageable);
}
