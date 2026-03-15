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

        Page<Enrollment> findAllByClassAndAcademicSchoolId(String classId, String academicYearId, String schoolId,
                        Pageable pageable);

        List<Enrollment> findAllByAcademicSchoolId(String academicYearId, String schoolId);

        Page<Enrollment> findAllByClassAndSchoolId(String classId, String schoolId, Pageable pageable);

        List<Enrollment> findAllByStreamIdAndAcademicYearIdAndSchoolId(String stream, String academicYearId,
                        String schoolId);

        Optional<Enrollment> findByStudentAndAcademicYearAndSchoolId(String studentId, String academicYearId,
                        String schoolId);

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
}
