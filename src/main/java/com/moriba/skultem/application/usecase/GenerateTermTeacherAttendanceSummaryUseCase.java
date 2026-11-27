package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TeacherAttendanceSummaryRowDTO;
import com.moriba.skultem.application.dto.TermTeacherAttendanceSummaryDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.TeacherAttendanceSummaryRowMapper;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.repository.TeacherAttendanceRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.service.AttendanceRateCalculator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GenerateTermTeacherAttendanceSummaryUseCase {

    private final com.moriba.skultem.application.services.TeacherAttendanceService teacherAttendanceService;
    private final TermRepository termRepo;
    private final TeacherAttendanceRepository attendanceRepo;
    private final ResolveTermSchoolDaysUseCase resolveTermSchoolDaysUseCase;

    public TermTeacherAttendanceSummaryDTO execute(String schoolId, String termId) {
        var term = termRepo.findByIdAndSchoolId(termId, schoolId)
                .orElseThrow(() -> new NotFoundException("Term not found"));

        // A section-limited admin sees only their own section's teachers, like the register and history.
        List<Teacher> activeTeachers = teacherAttendanceService.visibleActiveTeachers(schoolId);

        var rows = attendanceRepo.attendanceCountsByTeacherAndDateRange(schoolId, term.getStartDate(),
                term.getEndDate());

        int schoolDays = term.getAcademicYear() == null ? 0
                : resolveTermSchoolDaysUseCase.between(schoolId, term.getAcademicYear().getId(), term.getStartDate(),
                        term.getEndDate()).size();
        var teachers = TeacherAttendanceSummaryRowMapper.merge(activeTeachers, rows, schoolDays);

        long totalPresent = teachers.stream().mapToLong(TeacherAttendanceSummaryRowDTO::present).sum();
        long totalAbsent = teachers.stream().mapToLong(TeacherAttendanceSummaryRowDTO::absent).sum();
        long totalLate = teachers.stream().mapToLong(TeacherAttendanceSummaryRowDTO::late).sum();
        long totalExpected = (long) teachers.size() * schoolDays;

        double averageAttendance = AttendanceRateCalculator.rate(totalPresent + totalLate, totalExpected);

        return new TermTeacherAttendanceSummaryDTO(teachers, term.getName(), teachers.size(), averageAttendance,
                totalPresent, totalAbsent, totalLate, schoolDays);
    }
}
