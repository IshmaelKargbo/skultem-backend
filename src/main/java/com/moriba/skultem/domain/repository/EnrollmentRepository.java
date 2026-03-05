package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Enrollment;

public interface EnrollmentRepository {
    void save(Enrollment domain);

    Optional<Enrollment> findById(String id);

    Optional<Enrollment> findByIdAndSchoolId(String id, String schoolId);

    Optional<Enrollment> findByClassAndStudentAndSchoolId(String classId, String studentId, String schoolId);

    List<Enrollment> findAllByClassAndAcademicSchoolId(String classId, String academicYearId, String schoolId);

    List<Enrollment> findAllByAcademicSchoolId(String academicYearId, String schoolId);

    Page<Enrollment> findAllByClassAndSchoolId(String classId, String schoolId, Pageable pageable);

    List<Enrollment> findAllByStreamIdAndAcademicYearIdAndSchoolId(String stream, String academicYearId, String schoolId);

    Optional<Enrollment> findByStudentAndAcademicYearAndSchoolId(String studentId, String academicYearId,
            String schoolId);

    List<Enrollment> findAllByStudentIdAndClassIdAndSectionIdAndAcademicYearIdAndStreamIdAndSchoolId(String studentId,
            String classId, String sectionId, String academicYearId, String streamId, String schoolId);

    boolean existsByStudentIdAndClassIdAndSectionIdAndAcademicYearIdAndStreamIdAndSchoolId(String studentId,
            String classId, String sectionId, String academicYearId, String streamId, String schoolId);

    boolean existsByStudentIdAndAcademicYearIdAndSchoolId(String studentId, String academicYearId, String schoolId);

    boolean existsByStudentIdAndClassIdAndSectionIdAndAcademicYearIdAndSchoolIdAndStreamIdIsNull(String studentId,
            String classId, String sectionId, String academicYearId, String schoolId);

    Page<Enrollment> findBySchool(String schoolId, Pageable pageable);

    long countAll();
}
