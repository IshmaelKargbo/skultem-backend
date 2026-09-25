package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Collection;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.domain.model.Enrollment.Status;
import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.domain.vo.Level;
import com.moriba.skultem.infrastructure.persistence.entity.EnrollmentEntity;
import com.moriba.skultem.infrastructure.persistence.specs.FilterSpecificationBuilder;
import com.moriba.skultem.infrastructure.persistence.specs.PathResolver;

public interface EnrollmentJpaRepository extends JpaRepository<EnrollmentEntity, String>,
                JpaSpecificationExecutor<EnrollmentEntity> {

        List<EnrollmentEntity> findAllByClazz_IdAndSection_IdAndStream_IdAndAcademicYear_IdAndSchoolIdAndStatus(
                        String classId, String sectionId, String streamId, String academicYearId, String schoolId,
                        Status status);

        List<EnrollmentEntity> findAllByClazz_IdAndSection_IdAndStreamIsNullAndAcademicYear_IdAndSchoolIdAndStatus(
                        String classId, String sectionId, String academicYearId, String schoolId, Status status);

        boolean existsByClazz_IdAndSection_IdAndStream_IdAndAcademicYear_IdAndSchoolId(
                        String classId, String sectionId, String streamId, String academicYearId, String schoolId);

        boolean existsByClazz_IdAndSection_IdAndStreamIsNullAndAcademicYear_IdAndSchoolId(
                        String classId, String sectionId, String academicYearId, String schoolId);

        boolean existsByStudent_IdAndClazz_IdAndSection_IdAndAcademicYear_IdAndStream_IdAndSchoolId(
                        String studentId,
                        String classId,
                        String sectionId,
                        String academicYearId,
                        String streamId,
                        String schoolId);

        boolean existsByStudent_IdAndAcademicYear_IdAndSchoolId(String studentId, String academicYearId,
                        String schoolId);

        boolean existsByStudent_IdAndClazz_IdAndSection_IdAndAcademicYear_IdAndSchoolIdAndStreamIsNull(
                        String studentId,
                        String classId,
                        String sectionId,
                        String academicYearId,
                        String schoolId);

        Optional<EnrollmentEntity> findByIdAndSchoolId(String id, String schoolId);

        Optional<EnrollmentEntity> findByClazz_IdAndStudent_IdAndSchoolId(String classId, String studentId,
                        String schoolId);

        Optional<EnrollmentEntity> findByStudent_IdAndAcademicYear_IdAndSchoolId(String studentId,
                        String academicYearId,
                        String schoolId);

        Optional<EnrollmentEntity> findTopByStudent_IdAndSchoolIdOrderByCreatedAtDesc(String studentId,
                        String schoolId);

        List<EnrollmentEntity> findAllByStream_IdAndAcademicYear_IdAndSchoolIdAndStatusNot(String streamId,
                        String academicYearId, String schoolId, Status excluded);

        List<EnrollmentEntity> findAllByStudentIdAndClazz_IdAndSection_IdAndAcademicYear_IdAndStream_IdAndSchoolId(
                        String studentId,
                        String classId, String sectionId, String academicYearId, String streamId, String schoolId);

        List<EnrollmentEntity> findAllByAcademicYear_IdAndSchoolIdAndStatusNot(String academicYearId, String schoolId,
                        Status excluded);

        List<EnrollmentEntity> findAllBySchoolIdAndAcademicYear_IdAndCreatedAtBetween(String schoolId,
                        String academicYearId, Instant start, Instant end);

        List<EnrollmentEntity> findAllByStudentIdInAndAcademicYearIdAndSchoolId(List<String> studentIds,
                        String academicYearId, String schoolId);

        Page<EnrollmentEntity> findAllBySchoolId(String schoolId, Pageable pageable);

        // The roster finders below all take a Status to leave out (LEFT): a student who withdrew or
        // was expelled stays on file but isn't part of the class any more.
        Page<EnrollmentEntity> findAllByClazz_IdAndAcademicYear_IdAndSchoolIdAndStatusNot(String classId,
                        String academicYearId, String schoolId, Status excluded, Pageable pageable);

        List<EnrollmentEntity> findAllByStudent_IdAndSchoolIdAndStatus(String studentId, String schoolId, Status status);


        Page<EnrollmentEntity> findAllByClazzIdAndAcademicYearIdAndStreamIdAndStatusNot(String classId,
                        String academicYearId, String streamId, Status excluded, Pageable pageable);

        Page<EnrollmentEntity> findAllByClazz_IdAndSchoolIdAndStatusNot(String classId, String schoolId, Status excluded,
                        Pageable pageable);

        long countByAcademicYear_IdAndSchoolId(String academicYearId, String schoolId);

        long countBySchoolIdAndAcademicYear_IdAndCreatedAtBefore(String schoolId, String academicYearId, Instant date);

        // Scoped counterparts of the two methods above - levels always applied (full catalog for
        // whole-school callers), for the Dashboard's student-count/growth tiles.
        @Query("""
                    select count(e) from EnrollmentEntity e
                    where e.academicYear.id = :academicYearId and e.schoolId = :schoolId
                    and e.status <> com.moriba.skultem.domain.model.Enrollment.Status.LEFT
                    and e.clazz.level in :levels
                """)
        long countByAcademicYearAndSchoolIdAndLevels(@Param("academicYearId") String academicYearId,
                        @Param("schoolId") String schoolId, @Param("levels") Collection<Level> levels);

        @Query("""
                    select count(e) from EnrollmentEntity e
                    where e.schoolId = :schoolId and e.academicYear.id = :academicYearId
                    and e.createdAt < :date and e.clazz.level in :levels
                    and e.status <> com.moriba.skultem.domain.model.Enrollment.Status.LEFT
                """)
        long countBySchoolIdAndAcademicYearAndCreatedBeforeAndLevels(@Param("schoolId") String schoolId,
                        @Param("academicYearId") String academicYearId, @Param("date") Instant date,
                        @Param("levels") Collection<Level> levels);

        long countByStudent_IdAndClazz_IdAndSchoolIdAndStatus(String studentId, String classId, String schoolId,
                        Status status);

        // See EnrollmentRepository#demographicsByFilters.
        @Query("""
                            SELECT s.gender, s.religion, COUNT(DISTINCT e.student.id)
                            FROM EnrollmentEntity e
                            JOIN e.student s
                            JOIN e.clazz c
                            WHERE e.schoolId = :schoolId
                              AND e.status = com.moriba.skultem.domain.model.Enrollment.Status.ACTIVE
                              AND (:academicYearId IS NULL OR e.academicYear.id = :academicYearId)
                              AND (:classId IS NULL OR c.id = :classId)
                              AND (:level IS NULL OR c.level = :level)
                              AND c.level IN :levels
                            GROUP BY s.gender, s.religion
                        """)
        List<Object[]> demographicsByFilters(
                        @Param("schoolId") String schoolId,
                        @Param("academicYearId") String academicYearId,
                        @Param("classId") String classId,
                        @Param("level") Level level,
                        @Param("levels") Collection<Level> levels);

        default Page<EnrollmentEntity> runReport(String schoolId, List<Filter> filters, Collection<Level> levels,
                        Pageable pageable) {
                Specification<EnrollmentEntity> spec = (root, query, cb) -> cb.equal(root.get("schoolId"), schoolId);

                if (filters != null && !filters.isEmpty()) {
                        spec = spec.and(FilterSpecificationBuilder.build(filters));
                }

                // levels: always applied (full catalog for whole-school callers) - see SectionScope.
                spec = spec.and((root, query, cb) -> PathResolver.<EnrollmentEntity, Level>getPath(root, "clazz.level")
                        .in(levels));

                return findAll(spec, pageable);
        }
}