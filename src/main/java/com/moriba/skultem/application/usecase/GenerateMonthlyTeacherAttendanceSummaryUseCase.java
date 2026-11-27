package com.moriba.skultem.application.usecase;

import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TeacherAttendanceSummaryRowDTO;
import com.moriba.skultem.application.mapper.TeacherAttendanceSummaryRowMapper;
import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.repository.TeacherAttendanceRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GenerateMonthlyTeacherAttendanceSummaryUseCase {

    private final com.moriba.skultem.application.services.TeacherAttendanceService teacherAttendanceService;
    private final TeacherAttendanceRepository attendanceRepo;

    public List<TeacherAttendanceSummaryRowDTO> execute(String schoolId, YearMonth month) {
        // A section-limited admin sees only their own section's teachers, like the register and history.
        List<Teacher> activeTeachers = teacherAttendanceService.visibleActiveTeachers(schoolId);

        var rows = attendanceRepo.attendanceCountsByTeacherAndDateRange(schoolId, month.atDay(1),
                month.atEndOfMonth());

        int schoolDays = teacherAttendanceService.schoolDays(schoolId, month.atDay(1), month.atEndOfMonth()).size();
        return TeacherAttendanceSummaryRowMapper.merge(activeTeachers, rows, schoolDays);
    }
}
