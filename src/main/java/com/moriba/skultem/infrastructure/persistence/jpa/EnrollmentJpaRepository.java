package com.moriba.skultem.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.infrastructure.persistence.entity.EnrollmentEntity;
import com.moriba.skultem.infrastructure.persistence.specs.FilterSpecificationBuilder;

@Repository
public interface EnrollmentJpaRepository extends JpaRepository<EnrollmentEntity, String>,
                JpaSpecificationExecutor<EnrollmentEntity> {

        // ------------------- EXISTENCE CHECKS -------------------
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

        // ------------------- SIMPLE FINDERS -------------------
        Optional<EnrollmentEntity> findByIdAndSchoolId(String id, String schoolId);

        Optional<EnrollmentEntity> findByClazz_IdAndStudent_IdAndSchoolId(String classId, String studentId,
                        String schoolId);

        Optional<EnrollmentEntity> findByStudent_IdAndAcademicYear_IdAndSchoolId(String studentId,
                        String academicYearId,
                        String schoolId);

        List<EnrollmentEntity> findAllByStream_IdAndAcademicYear_IdAndSchoolId(String streamId, String academicYearId,
                        String schoolId);

        List<EnrollmentEntity> findAllByStudentIdAndClazz_IdAndSection_IdAndAcademicYear_IdAndStream_IdAndSchoolId(
                        String studentId,
                        String classId, String sectionId, String academicYearId, String streamId, String schoolId);

        List<EnrollmentEntity> findAllByAcademicYear_IdAndSchoolId(String academicYearId, String schoolId);

        List<EnrollmentEntity> findAllBySchoolIdAndAcademicYear_IdAndCreatedAtBetween(String schoolId,
                        String academicYearId, Instant start, Instant end);

        // ------------------- PAGINATION -------------------
        Page<EnrollmentEntity> findAllBySchoolId(String schoolId, Pageable pageable);

        Page<EnrollmentEntity> findAllByClazz_IdAndAcademicYear_IdAndSchoolId(String classId, String academicYearId,
                        String schoolId, Pageable pageable);

        Page<EnrollmentEntity> findAllByClazz_IdAndSchoolId(String classId, String schoolId, Pageable pageable);

        // ------------------- COUNTS -------------------
        long countByAcademicYear_IdAndSchoolId(String academicYearId, String schoolId);

        long countBySchoolIdAndAcademicYear_IdAndCreatedAtBefore(String schoolId, String academicYearId, Instant date);

        // ------------------- SPECIFICATION-BASED REPORT -------------------
        default Page<EnrollmentEntity> runReport(String schoolId, List<Filter> filters, Pageable pageable) {
                Specification<EnrollmentEntity> spec = (root, query, cb) -> cb.equal(root.get("schoolId"), schoolId);

                if (filters != null && !filters.isEmpty()) {
                        spec = spec.and(FilterSpecificationBuilder.build(filters));
                }

                return findAll(spec, pageable);
        }
}