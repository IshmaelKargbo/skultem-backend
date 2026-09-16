package com.moriba.skultem.application.usecase;

import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentAttendanceSummaryDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.AttendanceSummaryRowMapper;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GenerateMonthlyAttendanceSummaryUseCase {

    private final ClassSessionRepository classSessionRepo;
    private final SchoolRepository schoolRepo;
    private final AttendanceRepository attendanceRepo;

    // Scoped to one class SESSION (class + section + stream), not the whole class - "SSS 1
    // Science" and "SSS 1 Art" report separately, matching the same section/stream boundary
    // Daily Register already respects.
    public List<StudentAttendanceSummaryDTO> execute(String schoolId, String classSessionId, YearMonth month) {
        var classSession = classSessionRepo.findByIdAndSchoolId(classSessionId, schoolId)
                .orElseThrow(() -> new NotFoundException("Class session not found"));

        var school = schoolRepo.findById(schoolId).orElseThrow(() -> new NotFoundException("School not found"));

        String streamId = classSession.getStream() != null ? classSession.getStream().getId() : null;

        var rows = attendanceRepo.attendanceCountsBySessionAndDateRange(schoolId,
                classSession.getClazz().getId(), classSession.getSection().getId(), streamId,
                classSession.getAcademicYear().getId(), month.atDay(1), month.atEndOfMonth());

        return AttendanceSummaryRowMapper.toStudentSummaries(rows, classSession.getName(),
                school.getAttendanceThreshold());
    }
}
