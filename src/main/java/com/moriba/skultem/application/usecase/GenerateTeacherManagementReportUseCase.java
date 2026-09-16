package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TeacherManagementReportDTO;
import com.moriba.skultem.application.dto.TeacherManagementReportType;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.TeacherAttendanceService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// A thin dispatcher, not a fourth calculation path: every report type here delegates to the same
// use case/service an admin would reach directly from Mark Attendance/Daily Register/Monthly/
// Term Summary, so the numbers on a Management Report can never drift from what those pages show.
// Internal school management document - no MBSSE/Ministry claim is made anywhere in its output.
@Service
@Transactional
@RequiredArgsConstructor
public class GenerateTeacherManagementReportUseCase {

    private final TeacherAttendanceService teacherAttendanceService;
    private final GenerateMonthlyTeacherAttendanceSummaryUseCase generateMonthlyTeacherAttendanceSummaryUseCase;
    private final GenerateTermTeacherAttendanceSummaryUseCase generateTermTeacherAttendanceSummaryUseCase;

    public TeacherManagementReportDTO execute(String schoolId, TeacherManagementReportType reportType,
            String termId, LocalDate date, Integer year, Integer month) {
        Object payload = switch (reportType) {
            case DAILY_REGISTER -> {
                if (date == null) {
                    throw new RuleException("A date is required for the Daily Register report");
                }
                yield teacherAttendanceService.roster(schoolId, date);
            }
            case MONTHLY_SUMMARY -> {
                if (year == null || month == null) {
                    throw new RuleException("A year and month are required for the Monthly Summary report");
                }
                yield generateMonthlyTeacherAttendanceSummaryUseCase.execute(schoolId, YearMonth.of(year, month));
            }
            case TERM_SUMMARY -> {
                if (termId == null || termId.isBlank()) {
                    throw new RuleException("A term is required for the Term Summary report");
                }
                yield generateTermTeacherAttendanceSummaryUseCase.execute(schoolId, termId);
            }
        };

        return new TeacherManagementReportDTO(reportType, payload);
    }
}
