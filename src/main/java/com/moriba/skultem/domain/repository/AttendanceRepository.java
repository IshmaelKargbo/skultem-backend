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

    List<Object[]> attendanceCountsByClassGenderAndDateRange(String schoolId, String classId, String academicYearId,
            LocalDate startDate, LocalDate endDate);

    // Rows of [classId, className, Gender, presentOrLate, totalRecorded] for non-holiday records in the range.
    List<Object[]> attendanceCountsByClassAndGender(String schoolId, String academicYearId, String classId,
            Collection<Level> levels, LocalDate startDate, LocalDate endDate);

    Page<Attendance> runReport(String schoolId, List<Filter> filters, Collection<Level> levels,
            Pageable pageable);

    void deleteAllByEnrollmentIdAndSchoolId(String enrollmentId, String schoolId);
}
