package com.moriba.skultem.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.vo.Filter;

public interface EnrollmentRepository {
        void save(Enrollment domain);

        Optional<Enrollment> findById(String id);

        Optional<Enrollment> findByIdAndSchoolId(String id, String schoolId);

        Optional<Enrollment> findByClassAndStudentAndSchoolId(String classId, String studentId, String schoolId);

        Page<Enrollment> findAllByClassAndAcademicAndSchoolId(String classId, String academicYearId, String schoolId,
                        Pageable pageable);

        List<Enrollment> findAllByAcademicSchoolId(String academicYearId, String schoolId);

        List<Enrollment> findAllByStudentIdsAndAcademicYearAndSchoolId(List<String> studentIds, String academicYearId,
                        String schoolId);

        Page<Enrollment> findAllByClassAndSchoolId(String classId, String schoolId, Pageable pageable);

        List<Enrollment> findAllByStreamIdAndAcademicYearIdAndSchoolId(String stream, String academicYearId,
                        String schoolId);

        /**
         * Active roster of a specific class session (class + section + stream, streamId may be null) for a given
         * academic year, used to migrate students during promotion.
         */
        List<Enrollment> findActiveByClassIdAndSectionIdAndStreamIdAndAcademicYearIdAndSchoolId(String classId,
                        String sectionId, String streamId, String academicYearId, String schoolId);

        /**
         * Whether this class session has ever had any enrollment (any status) for the year - lets the
         * promotion roster tell "never had students" apart from "already promoted/left".
         */
        boolean existsAnyByClassIdAndSectionIdAndStreamIdAndAcademicYearIdAndSchoolId(String classId,
                        String sectionId, String streamId, String academicYearId, String schoolId);

        Optional<Enrollment> findByStudentAndAcademicYearAndSchoolId(String studentId, String academicYearId,
                        String schoolId);

        /**
         * Most recent enrollment on record for a student, regardless of academic year. Used as a fallback
         * when the student has no enrollment yet for the active academic year (e.g. right after year
         * rollover, before they've been re-enrolled/promoted) so they still show up with their last known
         * class and academic progress instead of disappearing from listings.
         */
        Optional<Enrollment> findTopByStudentAndSchoolIdOrderByCreatedAtDesc(String studentId, String schoolId);

        List<Enrollment> findAllByStudentIdAndClassIdAndSectionIdAndAcademicYearIdAndStreamIdAndSchoolId(
                        String studentId,
                        String classId, String sectionId, String academicYearId, String streamId, String schoolId);

        boolean existsByStudentIdAndClassIdAndSectionIdAndAcademicYearIdAndStreamIdAndSchoolId(String studentId,
                        String classId, String sectionId, String academicYearId, String streamId, String schoolId);

        boolean existsByStudentIdAndAcademicYearIdAndSchoolId(String studentId, String academicYearId, String schoolId);

        boolean existsByStudentIdAndClassIdAndSectionIdAndAcademicYearIdAndSchoolIdAndStreamIdIsNull(String studentId,
                        String classId, String sectionId, String academicYearId, String schoolId);

        Page<Enrollment> findBySchool(String schoolId, Pageable pageable);

        List<Enrollment> findBySchoolIdAndAcademicYearAndCreatedAtBetween(String schoolId, String academicYearId,
                        Instant start, Instant end);

        long countAll();

        long countByAcademicSchoolId(String academicYearId, String schoolId);

        long countBySchoolIdAndAcademicYearAndCreatedBefore(String schoolId, String academicYearId, Instant date);

        Page<Enrollment> runReport(String schoolId, List<Filter> filters, Pageable pageable);

        /**
         * How many times a student has already repeated a class - used to enforce the school's
         * max-repeat-count promotion rule.
         */
        long countByStudentIdAndClassIdAndSchoolIdAndStatus(String studentId, String classId, String schoolId,
                        Enrollment.Status status);
}
