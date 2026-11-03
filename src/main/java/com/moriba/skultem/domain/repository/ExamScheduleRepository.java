package com.moriba.skultem.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.ExamSchedule;

public interface ExamScheduleRepository {
    void save(ExamSchedule domain);

    void delete(ExamSchedule domain);

    Optional<ExamSchedule> findByIdAndSchoolId(String id, String schoolId);

    // Every paper in the school on that day - what a new or moved paper is checked against for clashes.
    List<ExamSchedule> findAllBySchoolIdAndExamDate(String schoolId, LocalDate examDate);
}
