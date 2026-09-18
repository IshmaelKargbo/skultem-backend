package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.ClassMaster;

public interface ClassMasterRepository {
    void save(ClassMaster domain);

    Optional<ClassMaster> findById(String id);

    Optional<ClassMaster> findByIdAndSchoolId(String id, String schoolId);

    // A session can now have more than one active (endedAt == null) class master - see
    // AssignTeacherToClassUseCase - so this returns every one of them, not a single Optional.
    List<ClassMaster> findAllActiveBySessionIdAndSchoolId(String sessionId, String schoolId);

    Page<ClassMaster> findByTeacherAndAcademicYear(String teacherId, String academicYearId, Pageable pageable);

    // True only if this exact teacher is CURRENTLY (not historically) a class master of this
    // session - a teacher who was removed and is being re-assigned must not be blocked by their
    // own past (ended) assignment.
    boolean existsByTeacherIdAndClassSessionIdAndSchoolId(String teacherId, String classSessionId, String schoolId);

    boolean existsByClassSessionIdAndSchoolId(String classSessionId, String schoolId);

    Optional<ClassMaster> findTopByClassSessionIdAndEndedAtIsNullOrderByAssignedAtDesc(String sessionId);

    Page<ClassMaster> findBySchool(String schoolId, Pageable pageable);

    long countAll();
}
