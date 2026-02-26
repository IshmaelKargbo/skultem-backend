package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.ClassSessionEntity;

@Repository
public interface ClassSessionJpaRepository extends JpaRepository<ClassSessionEntity, String> {
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

        Page<ClassSessionEntity> findAllBySchoolId(String schoolId, Pageable pageable);

        Page<ClassSessionEntity> findAllByClazz_Id(String classId, Pageable pageable);

        Page<ClassSessionEntity> findAllByAcademicYear_Id(String academicYearId, Pageable pageable);

        Page<ClassSessionEntity> findBySchoolIdAndAcademicYear_Id(String schoolId, String academicYearId,
                        Pageable pageable);

        Optional<ClassSessionEntity> findByClazz_IdAndAcademicYear_IdAndStream_Id(
                        String classId, String academicYearId, String streamId);

        List<ClassSessionEntity> findAllByClazz_IdAndAcademicYear_IdAndSchoolId(
                        String classId, String academicYearId, String schoolId);

        Optional<ClassSessionEntity> findByAcademicYear_IdAndClazz_IdAndSchoolId(String academic, String classId,
                        String schoolId);

        Optional<ClassSessionEntity> findByClazz_IdAndAcademicYear_IdAndSection_IdAndSchoolId(String classId,
                        String academicYearId, String sectionId, String schoolId);

        Optional<ClassSessionEntity> findByIdAndSchoolId(String id, String schoolId);

        Long countBySchoolId(String schoolId);
}
