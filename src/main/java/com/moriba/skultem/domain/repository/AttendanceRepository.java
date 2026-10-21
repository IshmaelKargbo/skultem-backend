package com.moriba.skultem.domain.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.application.dto.AttendanceHistoryDTO;
import com.moriba.skultem.domain.model.Attendance;
import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.domain.vo.Level;

public interface AttendanceRepository {

    void save(Attendance domain);

    void delete(Attendance domain);

    Optional<Attendance> findByIdAndSchoolId(String id, String schoolId);

    Optional<Attendance> findByEnrollmentAndDateAndSchoolId(String enrollmentId, LocalDate date, String schoolId);

    boolean existsByEnrollmentAndDateAndSchoolId(String enrollmentId, LocalDate date, String schoolId);

    Page<AttendanceHistoryDTO> fetchDailyClassAttendanceSummary(String classId, String sectionId, String streamId,
            String academicYear, String schoolId,
            Pageable pageable);

    Page<Attendance> findBySchoolId(String schoolId, Pageable pageable);

    Page<Attendance> findByEnrollmentAndSchoolId(String enrollmentId, String schoolId, Pageable pageable);

    List<Object[]> weeklyAttendance(String schoolId, LocalDate start, LocalDate end);

    List<Object[]> weeklyAttendance(String schoolId, LocalDate start, LocalDate end,
            Collection<Level> levels);

    List<Object[]> attendanceCountsByClassSince(String schoolId, String classId, String academicYearId,
            LocalDate since);

    // One row per recorded (non-holiday) day: [enrollmentId, date, 1 if present-or-late else 0].
    // classId == null means every class. Feeds AttendanceAttentionCalculator.
    List<Object[]> attendanceDaysSince(String schoolId, String classId, String academicYearId, LocalDate since);

    List<Object[]> attendanceCountsSinceForReport(String schoolId, String classId, String academicYearId,
            LocalDate since);

    List<Object[]> attendanceCountsBySessionAndDateRange(String schoolId, String classId, String sectionId,
            String streamId, String academicYearId, LocalDate startDate, LocalDate endDate);

    List<Object[]> attendanceCountsBySchoolAndDateRange(String schoolId, String academicYearId, LocalDate startDate,
            LocalDate endDate);

    // sectionId/streamId: optional narrowing within the class - null means any.
    List<Object[]> attendanceCountsByClassGenderAndDateRange(String schoolId, String classId, String sectionId,
            String streamId, String academicYearId, LocalDate startDate, LocalDate endDate);

    // Rows of [classId, className, sectionId, sectionName, streamId, streamName, Gender, presentOrLate,
    // totalRecorded] for non-holiday records in the range - streamId/streamName are null for no stream.
    // classId/sectionId/streamId are optional filters - null means any.
    List<Object[]> attendanceCountsByClassAndGender(String schoolId, String academicYearId, String classId,
            String sectionId, String streamId, Collection<Level> levels, LocalDate startDate, LocalDate endDate);

    Page<Attendance> runReport(String schoolId, List<Filter> filters, Collection<Level> levels,
            Pageable pageable);

    void deleteAllByEnrollmentIdAndSchoolId(String enrollmentId, String schoolId);

    // One row per enrollment of the year (classes at these levels; classId optionally narrows to one class),
    // including enrollments with no records: [studentId, enrolledAt (Instant), presentOrLate] over
    // non-holiday records in the range. A student who changed class has a row per enrollment.
    List<Object[]> presentCountsByStudent(String schoolId, String academicYearId, String classId,
            Collection<Level> levels, LocalDate startDate, LocalDate endDate);

    // One row per enrollment of the student's year, including ones with no records: [enrolledAt (Instant),
    // present (on time), late, excused, totalRecorded] over non-holiday records in the range.
    List<Object[]> statusCountsForStudent(String schoolId, String studentId, String academicYearId,
            LocalDate startDate, LocalDate endDate);

    // Rows of [classId, sectionId, streamId, date] - each day a class (class + section + stream) has any
    // non-holiday record in the range, i.e. the days it was marked. streamId is null for no stream.
    List<Object[]> markedDaysByClassSession(String schoolId, String academicYearId, Collection<Level> levels,
            LocalDate startDate, LocalDate endDate);
}
