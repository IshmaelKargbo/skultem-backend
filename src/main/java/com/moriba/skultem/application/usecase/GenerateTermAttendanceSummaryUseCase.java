package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentAttendanceSummaryDTO;
import com.moriba.skultem.application.dto.TermAttendanceSummaryDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.AttendanceSummaryRowMapper;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.service.AttendanceRateCalculator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GenerateTermAttendanceSummaryUseCase {

    private final ClassSessionRepository classSessionRepo;
    private final SchoolRepository schoolRepo;
    private final TermRepository termRepo;
    private final AttendanceRepository attendanceRepo;

    // Scoped to one class SESSION (class + section + stream), not the whole class - "SSS 1
    // Science" and "SSS 1 Art" report separately, matching the same section/stream boundary
    // Daily Register already respects.
    public TermAttendanceSummaryDTO execute(String schoolId, String classSessionId, String termId) {
        var classSession = classSessionRepo.findByIdAndSchoolId(classSessionId, schoolId)
                .orElseThrow(() -> new NotFoundException("Class session not found"));

        var school = schoolRepo.findById(schoolId).orElseThrow(() -> new NotFoundException("School not found"));

        var term = termRepo.findByIdAndSchoolId(termId, schoolId)
                .orElseThrow(() -> new NotFoundException("Term not found"));

        String streamId = classSession.getStream() != null ? classSession.getStream().getId() : null;

        var rows = attendanceRepo.attendanceCountsBySessionAndDateRange(schoolId,
                classSession.getClazz().getId(), classSession.getSection().getId(), streamId,
                classSession.getAcademicYear().getId(), term.getStartDate(), term.getEndDate());

        double threshold = school.getAttendanceThreshold();
        var students = AttendanceSummaryRowMapper.toStudentSummaries(rows, classSession.getName(), threshold);

        long totalPresent = students.stream().mapToLong(StudentAttendanceSummaryDTO::present).sum();
        long totalAbsent = students.stream().mapToLong(StudentAttendanceSummaryDTO::absent).sum();
        long totalLate = students.stream().mapToLong(StudentAttendanceSummaryDTO::late).sum();
        long totalRecorded = totalPresent + totalAbsent + totalLate;

        double averageAttendance = AttendanceRateCalculator.rate(totalPresent + totalLate, totalRecorded);
        int studentsBelowThreshold = (int) students.stream().filter(StudentAttendanceSummaryDTO::belowThreshold)
                .count();

        long totalBoys = students.stream().filter(s -> "MALE".equals(s.gender())).count();
        long totalGirls = students.stream().filter(s -> "FEMALE".equals(s.gender())).count();
        // "Present" here is attendance, not headcount - present+late, matching the same
        // present-or-late convention the percentage/threshold math uses everywhere else.
        long presentBoys = students.stream().filter(s -> "MALE".equals(s.gender()))
                .mapToLong(s -> s.present() + s.late()).sum();
        long presentGirls = students.stream().filter(s -> "FEMALE".equals(s.gender()))
                .mapToLong(s -> s.present() + s.late()).sum();

        return new TermAttendanceSummaryDTO(students, students.size(), averageAttendance, studentsBelowThreshold,
                totalPresent, totalAbsent, totalLate, totalBoys, totalGirls, presentBoys, presentGirls);
    }
}
