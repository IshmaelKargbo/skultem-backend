package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.ClassSubject;

public interface ClassSubjectRepository {
    void save(ClassSubject domain);

    Optional<ClassSubject> findById(String id);

    Optional<ClassSubject> findByClassIdAndSubjectId(String classId, String subjectId, String schoolId);

    boolean existsByClassAndSubject(String classId, String subjectId);

    boolean existsByClassIdAndSubjectIdAndSchoolId(String classId, String subjectId, String schoolId);

    Optional<ClassSubject> findByClassIdAndSubjectIdAndSchoolId(String classId, String subjectId, String schoolId);

    Page<ClassSubject> findByClass(String classId, Pageable pageable);

    Page<ClassSubject> findAllByClassIdAndSchoolId(String classId, String schoolId, Pageable pageble);

    Page<ClassSubject> findAllByClassIdAndStreamIdAndSchoolId(String classId, String streamId, String schoolId, Pageable pageble);

    Page<ClassSubject> findBySchool(String school, Pageable pageable);

    // Matches on class/subject/stream name, optionally narrowed to one class and/or mandatory
    // flag - backs the class subjects list's search box and filters.
    Page<ClassSubject> search(String school, String classId, Boolean mandatory, String query, Pageable pageable);

    void delete(ClassSubject domain);
}
