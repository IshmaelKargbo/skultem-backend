package com.moriba.skultem.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.application.dto.AttendanceHistoryDTO;
import com.moriba.skultem.domain.model.Attendance;
import com.moriba.skultem.domain.vo.Filter;

public interface AttendanceRepository {
    void save(Attendance domain);

    void delete(Attendance domain);

    Optional<Attendance> findByIdAndSchoolId(String id, String schoolId);

    Optional<Attendance> findByEnrollmentAndDateAndSchoolId(String enrollmentId, LocalDate date, String schoolId);

    boolean existsByEnrollmentAndDateAndSchoolId(String enrollmentId, LocalDate date, String schoolId);

    Page<AttendanceHistoryDTO> fetchDailyClassAttendanceSummary(String classId, String academicYear, String schoolId,
            Pageable pageable);

    Page<Attendance> findBySchoolId(String schoolId, Pageable pageable);

    Page<Attendance> findByEnrollmentAndSchoolId(String enrollmentId, String schoolId, Pageable pageable);

    List<Object[]> weeklyAttendance(String schoolId, LocalDate start, LocalDate end);

    // Row shape: [enrollmentId (String), presentCount (Long), totalCount (Long)] - see
    // ComputeClassAttentionUseCase.
    List<Object[]> attendanceCountsByClassSince(String schoolId, String classId, String academicYearId,
            LocalDate since);

    // Same as attendanceCountsByClassSince but classId nullable (whole school) - the Students
    // Requiring Attention report's school-wide variant. Kept separate from
    // attendanceCountsByClassSince so ComputeClassAttentionUseCase's existing contract is
    // untouched.
    List<Object[]> attendanceCountsSinceForReport(String schoolId, String classId, String academicYearId,
            LocalDate since);

    // Per-student attendance counts for one class SESSION (class + section + stream, matching the
    // same section/stream scoping GetClassSessionAttendanceUseCase's roster uses) over an explicit
    // date range - backs Monthly Summary, Term Summary and the matching Inspection Report types.
    // A null streamId matches enrollments with no stream (the "no stream" class session), same
    // null-safe convention as loadSessionEnrollments. Row shape: [enrollmentId, studentId,
    // givenNames, familyName, admissionNumber, gender (Gender), presentOrLateCount (Long),
    // lateCount (Long), totalRecorded (Long)].
    List<Object[]> attendanceCountsBySessionAndDateRange(String schoolId, String classId, String sectionId,
            String streamId, String academicYearId, LocalDate startDate, LocalDate endDate);

    // Per-student attendance counts across EVERY class session in the school for one date range -
    // backs Class Summary (one row per class session, computed by grouping these rows in Java
    // rather than issuing one query per class session). Row shape: [classId, className, sectionId,
    // sectionName, streamId (nullable), streamName (nullable), enrollmentId, studentId, gender
    // (Gender), presentOrLateCount (Long), lateCount (Long), totalRecorded (Long)].
    List<Object[]> attendanceCountsBySchoolAndDateRange(String schoolId, String academicYearId, LocalDate startDate,
            LocalDate endDate);

    // Per-day, per-gender attendance counts for one class over a date range (typically one week) -
    // backs the Weekly Attendance by Gender report. Row shape: [date (LocalDate), gender (Gender),
    // presentOrLateCount (Long), totalRecorded (Long)]. Boys/girls enrolled totals are NOT part of
    // this row shape - the use case sources those from the class roster (EnrollmentRepository), not
    // from attendance records, so a day nobody marked attendance still reports the correct
    // denominator instead of silently showing zero enrolled.
    List<Object[]> attendanceCountsByClassGenderAndDateRange(String schoolId, String classId, String academicYearId,
            LocalDate startDate, LocalDate endDate);

    Page<Attendance> runReport(String schoolId, List<Filter> filters, Pageable pageable);
}
