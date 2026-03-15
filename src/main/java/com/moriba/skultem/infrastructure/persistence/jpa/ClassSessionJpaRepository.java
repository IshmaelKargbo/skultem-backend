package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.infrastructure.persistence.entity.ClassSessionEntity;
import com.moriba.skultem.infrastructure.persistence.specs.FilterSpecificationBuilder;

@Repository
public interface ClassSessionJpaRepository
        extends JpaRepository<ClassSessionEntity, String>, JpaSpecificationExecutor<ClassSessionEntity> {
    boolean existsByClazz_IdAndAcademicYear_IdAndStream_IdAndSchoolId(String classId, String academicYear,
            String streamId,
            String schoolId);

    boolean existsByClazz_IdAndAcademicYear_IdAndStreamIsNullAndSchoolId(String classId, String academicYear,
            String schoolId);

    boolean existsByClazz_IdAndAcademicYear_IdAndSection_IdAndStream_IdAndSchoolId(String classId,
            String academicYearId, String sectionId, String streamId, String schoolId);

    boolean existsByClazz_IdAndAcademicYear_IdAndSection_IdAndStreamIsNullAndSchoolId(String classId,
            String academicYearId, String sectionId, String schoolId);

    Optional<ClassSessionEntity> findByClazz_IdAndAcademicYear_IdAndSchoolId(String classId, String academicYearId,
            String schoolId);

    Optional<ClassSessionEntity> findByClazz_IdAndAcademicYear_IdAndSection_IdAndStream_IdAndSchoolId(
            String classId,
            String academicYearId, String sectionId, String streamId, String schoolId);

    @Query("""
                SELECT cs FROM ClassSessionEntity cs
                WHERE cs.schoolId = :schoolId
                AND cs.academicYear.id = :academicYearId
                AND NOT EXISTS (
                    SELECT cm FROM ClassMasterEntity cm
                    WHERE cm.session.id = cs.id
                    AND cm.endedAt IS NULL
                )
            """)
    Page<ClassSessionEntity> findUnassignedBySchoolAndAcademicYearOrderByClazz_LevelOrderAsc(
            @Param("schoolId") String schoolId,
            @Param("academicYearId") String academicYearId,
            Pageable pageable);

    Page<ClassSessionEntity> findAllBySchoolIdOrderByClazz_LevelOrderAsc(String schoolId, Pageable pageable);

    Page<ClassSessionEntity> findAllByClazz_IdOrderByClazz_LevelOrderAsc(String classId, Pageable pageable);

    Page<ClassSessionEntity> findAllByAcademicYear_IdOrderByClazz_LevelOrderAsc(String academicYearId,
            Pageable pageable);

    Page<ClassSessionEntity> findAllBySchoolIdAndAcademicYear_IdOrderByClazz_LevelOrderAsc(String schoolId,
            String academicYearId,
            Pageable pageable);

    Optional<ClassSessionEntity> findByClazz_IdAndAcademicYear_IdAndStream_Id(
            String classId, String academicYearId, String streamId);

    List<ClassSessionEntity> findAllByClazz_IdAndAcademicYear_IdAndSchoolIdOrderByClazz_LevelOrderAsc(
            String classId, String academicYearId, String schoolId);

    Optional<ClassSessionEntity> findByAcademicYear_IdAndClazz_IdAndSchoolId(String academic, String classId,
            String schoolId);

    Optional<ClassSessionEntity> findByClazz_IdAndAcademicYear_IdAndSection_IdAndSchoolId(String classId,
            String academicYearId, String sectionId, String schoolId);

    Optional<ClassSessionEntity> findByIdAndSchoolId(String id, String schoolId);

    Long countBySchoolId(String schoolId);

    default Page<ClassSessionEntity> runReport(String schoolId, List<Filter> filters, Pageable pageable) {
        Specification<ClassSessionEntity> spec = (root, query, cb) -> cb.equal(root.get("schoolId"), schoolId);

        if (filters != null && !filters.isEmpty()) {
            spec = spec.and(FilterSpecificationBuilder.build(filters));
        }

        return findAll(spec, pageable);
    }
}
