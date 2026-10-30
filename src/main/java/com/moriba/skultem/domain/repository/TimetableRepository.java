package com.moriba.skultem.domain.repository;

import com.moriba.skultem.domain.model.Timetable;

import java.util.List;
import java.util.Optional;

public interface TimetableRepository {
    void save(Timetable domain);

    // Every subject in one period/day slot, oldest first. Usually one; more when students split
    // between options (e.g. PHE or RME).
    List<Timetable> findAllByPeriodIdAndDayIdAndSchoolId(String period, String day, String schoolId);

    Optional<Timetable> findByIdAndSchoolId(String id, String schoolId);

    List<Timetable> findAllByPeriodId(String period);

    // Every slot for one subject in one academic year (Library Attendance's scheduled sessions).
    List<Timetable> findAllBySubjectAndAcademicYear(String subjectId, String academicYearId, String schoolId);

    boolean existsByPeriodAndDayAndSchoolId(String period, String day, String schoolId);

    void delete(Timetable domain);
}
