package com.moriba.skultem.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.TeacherAttendance;

public interface TeacherAttendanceRepository {
    void save(TeacherAttendance domain);

    void saveAll(List<TeacherAttendance> domains);

    Optional<TeacherAttendance> findByTeacherIdAndSchoolIdAndDate(String teacherId, String schoolId, LocalDate date);

    List<TeacherAttendance> findAllBySchoolIdAndDate(String schoolId, LocalDate date);

    List<TeacherAttendance> findAllBySchoolId(String schoolId);

    // One teacher's own attendance history within a range - powers the calendar on their
    // profile page, as opposed to the whole-school roster/history views above.
    List<TeacherAttendance> findAllBySchoolIdAndTeacherIdBetween(String schoolId, String teacherId, LocalDate from,
            LocalDate to);
}
