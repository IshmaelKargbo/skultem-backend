package com.moriba.skultem.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.ExamPaper;

public interface ExamPaperRepository {
    void save(ExamPaper domain);

    void delete(ExamPaper domain);

    Optional<ExamPaper> findByIdAndSchoolId(String id, String schoolId);

    List<ExamPaper> findAllByTimetable(String timetableId);

    // Every paper in the school on that day, on any timetable - what a new or moved paper is checked against.
    List<ExamPaper> findAllBySchoolIdAndExamDate(String schoolId, LocalDate examDate);
}
