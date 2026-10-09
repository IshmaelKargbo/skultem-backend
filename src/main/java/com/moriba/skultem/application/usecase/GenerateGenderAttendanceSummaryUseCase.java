package com.moriba.skultem.application.usecase;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.GenderAttendanceSummaryDTO;
import com.moriba.skultem.application.dto.GenderAttendanceSummaryDTO.ClassGenderAttendanceDTO;
import com.moriba.skultem.application.dto.GenderAttendanceSummaryDTO.GenderAttendanceTotalsDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.services.SectionScopeService;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.service.AttendanceRateCalculator;
import com.moriba.skultem.domain.vo.Gender;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Boys/girls attendance totals for a week, month, term or academic year, whole school and per class,
// built from the same daily attendance records as every other attendance report (present-or-late over
// recorded non-holiday days, see AttendanceRateCalculator) - nothing is entered or stored separately.
@Service
@Transactional
@RequiredArgsConstructor
public class GenerateGenderAttendanceSummaryUseCase {

    public enum Period {
        WEEK, MONTH, TERM, YEAR
    }

    private final AttendanceRepository attendanceRepo;
    private final TermRepository termRepo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;
    private final SectionScopeService sectionScopeService;

    public GenderAttendanceSummaryDTO execute(String schoolId, String academicYearId, Period period, LocalDate date,
            String termId, String classId) {
        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

        LocalDate start;
        LocalDate end;
        switch (period) {
            case WEEK -> {
                start = date.with(DayOfWeek.MONDAY);
                end = start.plusDays(6);
            }
            case MONTH -> {
                start = date.withDayOfMonth(1);
                end = date.withDayOfMonth(date.lengthOfMonth());
            }
            case TERM -> {
                Term term = resolveTerm(schoolId, academicYear.getId(), termId, date);
                start = term.getStartDate();
                end = term.getEndDate();
            }
            default -> {
                start = academicYear.getStartDate();
                end = academicYear.getEndDate();
            }
        }

        var rows = attendanceRepo.attendanceCountsByClassAndGender(schoolId, academicYear.getId(), classId,
                sectionScopeService.levels(), start, end);

        // [boysPresent, girlsPresent, boysRecorded, girlsRecorded]
        Map<String, long[]> byClass = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();
        long[] school = new long[4];

        for (Object[] row : rows) {
            String cid = (String) row[0];
            names.put(cid, (String) row[1]);
            Gender gender = (Gender) row[2];
            long present = ((Number) row[3]).longValue();
            long recorded = ((Number) row[4]).longValue();

            int offset = gender == Gender.MALE ? 0 : 1;
            long[] counts = byClass.computeIfAbsent(cid, k -> new long[4]);
            counts[offset] += present;
            counts[offset + 2] += recorded;
            school[offset] += present;
            school[offset + 2] += recorded;
        }

        List<ClassGenderAttendanceDTO> classes = new ArrayList<>();
        byClass.forEach((cid, counts) -> classes.add(new ClassGenderAttendanceDTO(cid, names.get(cid), totals(counts))));

        return new GenderAttendanceSummaryDTO(period.name(), start, end, totals(school), classes);
    }

    private Term resolveTerm(String schoolId, String academicYearId, String termId, LocalDate date) {
        if (termId != null && !termId.isBlank()) {
            return termRepo.findByIdAndAcademicYearIdAndSchoolId(termId, academicYearId, schoolId)
                    .orElseThrow(() -> new NotFoundException("Term not found"));
        }

        var terms = termRepo.findByAcademicYearIdAndSchool(academicYearId, schoolId);
        return terms.stream()
                .filter(t -> t.getStartDate() != null && t.getEndDate() != null
                        && !date.isBefore(t.getStartDate()) && !date.isAfter(t.getEndDate()))
                .findFirst()
                .or(() -> termRepo.findActiveBySchoolAndAcademicYear(schoolId, academicYearId))
                .orElseThrow(() -> new NotFoundException("No term found for this date"));
    }

    private GenderAttendanceTotalsDTO totals(long[] c) {
        return new GenderAttendanceTotalsDTO(c[0], c[1], c[2], c[3],
                AttendanceRateCalculator.rate(c[0], c[2]),
                AttendanceRateCalculator.rate(c[1], c[3]),
                AttendanceRateCalculator.rate(c[0] + c[1], c[2] + c[3]));
    }
}
