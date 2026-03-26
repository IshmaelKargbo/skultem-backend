package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.ClassMaster;

public interface ClassMasterRepository {
    void save(ClassMaster domain);

    Optional<ClassMaster> findById(String id);

    Optional<ClassMaster> findByIdAndSchoolId(String id, String schoolId);

    Optional<ClassMaster> findBySessionIdAndSchoolId(String sessionId, String schoolId);

    Page<ClassMaster> findByTeacherAndAcademicYear(String teacherId, String academicYearId, Pageable pageable);

    boolean existsByTeacherIdAndClassSessionIdAndSchoolId(String teacherId, String classSessionId, String schoolId);

    boolean existsByClassSessionIdAndSchoolId(String classSessionId, String schoolId);

    Optional<ClassMaster> findTopByClassSessionIdAndEndedAtIsNullOrderByAssignedAtDesc(String sessionId);

    Page<ClassMaster> findBySchool(String schoolId, Pageable pageable);

    long countAll();
}
