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
import com.moriba.skultem.domain.vo.Level;

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
            String termId, String classId, String sectionId, String streamId, Level level) {
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

        // level narrows the caller's own section scope - it can never widen it.
        var levels = sectionScopeService.levels().stream()
                .filter(l -> level == null || l == level)
                .toList();
        List<Object[]> rows = levels.isEmpty() ? List.of()
                : attendanceRepo.attendanceCountsByClassAndGender(schoolId, academicYear.getId(), classId,
                        sectionId, streamId, levels, start, end);

        // One entry per class + section + stream, in the query's order (class rank, then section, then stream).
        // counts: [boysPresent, girlsPresent, boysRecorded, girlsRecorded]
        Map<String, long[]> byGroup = new LinkedHashMap<>();
        Map<String, Object[]> groups = new LinkedHashMap<>();
        long[] school = new long[4];

        for (Object[] row : rows) {
            String key = row[0] + "|" + row[2] + "|" + row[4];
            groups.putIfAbsent(key, row);
            Gender gender = (Gender) row[6];
            long present = ((Number) row[7]).longValue();
            long recorded = ((Number) row[8]).longValue();

            int offset = gender == Gender.MALE ? 0 : 1;
            long[] counts = byGroup.computeIfAbsent(key, k -> new long[4]);
            counts[offset] += present;
            counts[offset + 2] += recorded;
            school[offset] += present;
            school[offset + 2] += recorded;
        }

        List<ClassGenderAttendanceDTO> classes = new ArrayList<>();
        byGroup.forEach((key, counts) -> {
            Object[] g = groups.get(key);
            classes.add(new ClassGenderAttendanceDTO((String) g[0], (String) g[1], (String) g[2], (String) g[3],
                    (String) g[4], (String) g[5], totals(counts)));
        });

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
