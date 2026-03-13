package com.moriba.skultem.infrastructure.persistence.jpa;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.vo.Gender;
import com.moriba.skultem.infrastructure.persistence.entity.EnrollmentEntity;

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
        default Page<EnrollmentEntity> runStudentReport(
                        String schoolId,
                        String academicYearId,
                        String classId,
                        String sectionId,
                        String streamId,
                        Enrollment.Status status,
                        Gender gender,
                        String studentName,
                        String admissionNumber,
                        LocalDate dobFrom,
                        LocalDate dobTo,
                        Pageable pageable) {

                Specification<EnrollmentEntity> spec = Specification
                                .where((root, query, cb) -> cb.equal(root.get("schoolId"), schoolId));

                if (academicYearId != null) {
                        spec = spec.and((root, query, cb) -> cb.equal(root.get("academicYear").get("id"),
                                        academicYearId));
                }

                if (classId != null) {
                        spec = spec.and((root, query, cb) -> cb.equal(root.get("clazz").get("id"), classId));
                }

                if (sectionId != null) {
                        spec = spec.and((root, query, cb) -> cb.equal(root.get("section").get("id"), sectionId));
                }

                if (streamId != null) {
                        spec = spec.and((root, query, cb) -> cb.equal(root.get("stream").get("id"), streamId));
                }

                if (status != null) {
                        spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
                }

                if (gender != null) {
                        spec = spec.and((root, query, cb) -> cb.equal(root.get("student").get("gender"), gender));
                }

                if (studentName != null && !studentName.isBlank()) {
                        String pattern = "%" + studentName.toLowerCase() + "%";
                        spec = spec.and((root, query, cb) -> cb.or(
                                        cb.like(cb.lower(root.get("student").get("familyName")), pattern),
                                        cb.like(cb.lower(root.get("student").get("givenNames")), pattern)));
                }

                if (admissionNumber != null) {
                        spec = spec.and((root, query, cb) -> cb.equal(root.get("student").get("admissionNumber"),
                                        admissionNumber));
                }

                if (dobFrom != null) {
                        spec = spec.and((root, query, cb) -> cb
                                        .greaterThanOrEqualTo(root.get("student").get("dateOfBirth"), dobFrom));
                }

                if (dobTo != null) {
                        spec = spec.and((root, query, cb) -> cb
                                        .lessThanOrEqualTo(root.get("student").get("dateOfBirth"), dobTo));
                }

                return findAll(spec, pageable);
        }
}