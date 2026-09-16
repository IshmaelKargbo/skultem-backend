package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.InspectionReportDTO;
import com.moriba.skultem.application.dto.InspectionReportType;
import com.moriba.skultem.application.error.RuleException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// A thin dispatcher, not a fourth calculation path: every report type here delegates to the same
// use case a school admin would reach directly from its own Attendance sub-nav page, so the
// numbers on an Inspection Report can never drift from what Daily Register/Monthly/Term Summary
// already show. The "Class" filter is always a class session (matching every other attendance
// page's class dropdown), and Monthly/Term Summary now stay scoped to that same session (section
// + stream) rather than rolling the whole class up - "SSS 1 Science" and "SSS 1 Art" are reported
// on separately, the class session itself already pins the academic year.
@Service
@Transactional
@RequiredArgsConstructor
public class GenerateInspectionReportUseCase {

    private final GenerateDailyAttendanceRegisterUseCase generateDailyAttendanceRegisterUseCase;
    private final GenerateMonthlyAttendanceSummaryUseCase generateMonthlyAttendanceSummaryUseCase;
    private final GenerateTermAttendanceSummaryUseCase generateTermAttendanceSummaryUseCase;

    public InspectionReportDTO execute(String schoolId, InspectionReportType reportType, String classSessionId,
            String termId, LocalDate date, Integer year, Integer month) {
        Object payload = switch (reportType) {
            case DAILY_REGISTER -> {
                if (date == null) {
                    throw new RuleException("A date is required for the Daily Register report");
                }
                yield generateDailyAttendanceRegisterUseCase.execute(schoolId, classSessionId, date);
            }
            case MONTHLY_SUMMARY -> {
                if (year == null || month == null) {
                    throw new RuleException("A year and month are required for the Monthly Summary report");
                }
                yield generateMonthlyAttendanceSummaryUseCase.execute(schoolId, classSessionId,
                        YearMonth.of(year, month));
            }
            case TERM_SUMMARY -> {
                if (termId == null || termId.isBlank()) {
                    throw new RuleException("A term is required for the Term Summary report");
                }
                yield generateTermAttendanceSummaryUseCase.execute(schoolId, classSessionId, termId);
            }
        };

        return new InspectionReportDTO(reportType, payload);
    }
}
