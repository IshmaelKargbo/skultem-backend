package com.moriba.skultem.domain.repository;

import com.moriba.skultem.domain.model.SchemeOfWork;
import com.moriba.skultem.domain.model.Week;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SchemeOfWorkRepository {
    void save(SchemeOfWork domain);

    Optional<SchemeOfWork> findById(String id);

    Optional<SchemeOfWork> findBySubjectAndTermAndSession(String subjectId, String termId, String sessionId, String schoolId);

    Page<SchemeOfWork> findAllBySchoolId(String school, Pageable pageable);

    Page<SchemeOfWork> findAllByTeacherIdAndSchoolId(String teacherId, String school, Pageable pageable);

    Page<SchemeOfWork> search(String school, String subjectId, String sessionId, String termId, Week.State progress, Pageable pageable);

    Page<SchemeOfWork> searchByTeacher(String teacherId, String school, String subjectId, String sessionId, String termId, Week.State progress, Pageable pageable);

    boolean existsBySubjectAndTermAndSession(String subjectId, String termId, String sessionId, String schoolId);
}
