package com.moriba.skultem.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.domain.vo.Level;

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

        Page<Enrollment> findAllByClassIdAndStreamIdAndAcademicYearId(String classId, String stream, String academicYearId, Pageable pageable);

        List<Enrollment> findActiveByClassIdAndSectionIdAndStreamIdAndAcademicYearIdAndSchoolId(String classId,
                        String sectionId, String streamId, String academicYearId, String schoolId);

        boolean existsAnyByClassIdAndSectionIdAndStreamIdAndAcademicYearIdAndSchoolId(String classId,
                        String sectionId, String streamId, String academicYearId, String schoolId);

        Optional<Enrollment> findByStudentAndAcademicYearAndSchoolId(String studentId, String academicYearId,
                        String schoolId);

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

        long countByStudentIdAndClassIdAndSchoolIdAndStatus(String studentId, String classId, String schoolId,
                        Enrollment.Status status);

        // Row shape: [gender (Gender), religion (String), studentCount (Long)] - one row per
        // distinct (gender, religion) combination among ACTIVE enrollments matching the given
        // filters (academicYearId/classId/level all nullable = no filter on that dimension). Backs
        // the student demographics report; religion is left as raw text here and bucketed into the
        // school's canonical categories (Muslim/Christian/Other/Not specified) by the use case, not
        // this query, since it's free text rather than an enum.
        List<Object[]> demographicsByFilters(String schoolId, String academicYearId, String classId, Level level);

    /** Wipes every row for this school - see WipeTestSchoolDataUseCase. */
    void deleteAllBySchoolId(String schoolId);
}
