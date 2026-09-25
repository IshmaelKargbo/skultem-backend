package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.ClassSubject;

public interface ClassSubjectRepository {
    void save(ClassSubject domain);

    Optional<ClassSubject> findById(String id);

    Optional<ClassSubject> findByClassIdAndSubjectId(String classId, String subjectId, String schoolId);

    Optional<ClassSubject> findByClassIdAndSubjectIdAndStramId(String classId, String subjectId, String streamId);

    boolean existsByClassAndSubject(String classId, String subjectId);

    boolean existsByClassIdAndSubjectIdAndSchoolId(String classId, String subjectId, String schoolId);

    Optional<ClassSubject> findByClassIdAndSubjectIdAndSchoolId(String classId, String subjectId, String schoolId);

    Page<ClassSubject> findByClass(String classId, Pageable pageable);

    Page<ClassSubject> findAllByClassIdAndSchoolId(String classId, String schoolId, Pageable pageble);

    Page<ClassSubject> findAllByClassIdAndStreamIdAndSchoolId(String classId, String streamId, String schoolId, Pageable pageble);

    Page<ClassSubject> findBySchool(String school, Pageable pageable);

    // levels: only class subjects of classes at these levels (pass every level for no restriction).
    Page<ClassSubject> search(String school, String classId, Boolean mandatory, String query,
            java.util.Collection<com.moriba.skultem.domain.vo.Level> levels, Pageable pageable);

    void delete(ClassSubject domain);
}
