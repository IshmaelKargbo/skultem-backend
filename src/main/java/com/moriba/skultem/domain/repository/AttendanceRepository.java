package com.moriba.skultem.domain.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.application.dto.AttendanceHistoryDTO;
import com.moriba.skultem.domain.model.Attendance;

public interface AttendanceRepository {
    void save(Attendance domain);

    void delete(Attendance domain);

    Optional<Attendance> findByIdAndSchoolId(String id, String schoolId);

    Optional<Attendance> findByEnrollmentAndDateAndSchoolId(String enrollmentId, LocalDate date, String schoolId);

    boolean existsByEnrollmentAndDateAndSchoolId(String enrollmentId, LocalDate date, String schoolId);

    Page<AttendanceHistoryDTO> fetchDailyClassAttendanceSummary(String classId, String academicYear, String schoolId, Pageable pageable);

    Page<Attendance> findBySchoolId(String schoolId, Pageable pageable);

    Page<Attendance> findByEnrollmentAndSchoolId(String enrollmentId, String schoolId, Pageable pageable);
}
