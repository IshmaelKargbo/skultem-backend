package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.ExamTimetable;

public interface ExamTimetableRepository {
    void save(ExamTimetable domain);

    void delete(ExamTimetable domain);

    Optional<ExamTimetable> findByIdAndSchoolId(String id, String schoolId);

    Optional<ExamTimetable> findByTermAndAssessment(String schoolId, String termId, String assessmentId);

    List<ExamTimetable> findAllBySchoolAndTerm(String schoolId, String termId);
}
