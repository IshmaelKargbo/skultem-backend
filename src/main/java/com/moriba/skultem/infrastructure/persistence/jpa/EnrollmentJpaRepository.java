package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.EnrollmentEntity;

@Repository
public interface EnrollmentJpaRepository extends JpaRepository<EnrollmentEntity, String> {

        boolean existsByStudent_IdAndClazz_IdAndSection_IdAndAcademicYear_IdAndStream_IdAndSchoolId(String studentId,
                        String classId, String sectionId, String academicYearId, String streamId, String schoolId);

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

        Page<EnrollmentEntity> findAllBySchoolId(String schoolId, Pageable pageable);

        Page<EnrollmentEntity> findAllByClazz_IdAndAcademicYear_IdAndSchoolId(String classId, String academicYearId,
                        String schoolId, Pageable pageable);

        List<EnrollmentEntity> findAllByStream_IdAndAcademicYear_IdAndSchoolId(String streamId, String academicYearId,
                        String schoolId);

        List<EnrollmentEntity> findAllByStudentIdAndClazz_IdAndSection_IdAndAcademicYear_IdAndStream_IdAndSchoolId(
                        String studentId,
                        String classId, String sectionId, String academicYearId, String streamId, String schoolId);

        List<EnrollmentEntity> findAllByAcademicYear_IdAndSchoolId(String academicYearId, String schoolId);

        Page<EnrollmentEntity> findAllByClazz_IdAndSchoolId(String classId, String schoolId, Pageable pageable);
}
