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
// built from the same daily attendance records as every other attendance report. Like the students list,
// every enrolled student is measured against every school day so far (ResolveTermSchoolDaysUseCase#between):
// a day with no mark - or a student never marked - counts as missed. Nothing is entered or stored separately.
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
    private final ResolveTermSchoolDaysUseCase resolveTermSchoolDaysUseCase;
    private final com.moriba.skultem.domain.repository.EnrollmentRepository enrollmentRepo;

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

        long schoolDays = resolveTermSchoolDaysUseCase.between(schoolId, academicYear.getId(), start, end).size();
        rows = levels.isEmpty() ? rows : withUnmarked(rows, schoolId, academicYear.getId(), classId, sectionId,
                streamId, levels);

        // One entry per class + section + stream, in the query's order (class rank, then section, then stream).
        // counts: [boysPresent, girlsPresent, boysExpected, girlsExpected] - expected = school days per student.
        Map<String, long[]> byGroup = new LinkedHashMap<>();
        Map<String, Object[]> groups = new LinkedHashMap<>();
        long[] school = new long[4];

        for (Object[] row : rows) {
            String key = row[0] + "|" + row[2] + "|" + row[4];
            groups.putIfAbsent(key, row);
            Gender gender = (Gender) row[6];
            // Capped: a record on a day later declared closed can't push anyone past 100%.
            long present = Math.min(((Number) row[7]).longValue(), schoolDays);
            long recorded = schoolDays;

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

    // The query's rows (one per enrollment) plus an all-zero row for each active enrollment matching the same
    // filters that has no record in the period, kept in class / section / stream order.
    private List<Object[]> withUnmarked(List<Object[]> rows, String schoolId, String academicYearId, String classId,
            String sectionId, String streamId, List<Level> levels) {
        var seen = new java.util.HashSet<String>();
        rows.forEach(r -> seen.add((String) r[9]));
        var order = new java.util.HashMap<String, Integer>();
        var sectionOrder = new java.util.HashMap<String, Integer>();
        var all = new ArrayList<>(rows);
        boolean added = false;
        for (var e : enrollmentRepo.findAllByAcademicSchoolId(academicYearId, schoolId)) {
            var c = e.getClazz();
            var sec = e.getSection();
            if (c != null) order.put(c.getId(), c.getDisplayOrder());
            if (sec != null) sectionOrder.put(sec.getId(), sec.getDisplayOrder());
            if (e.getStatus() != com.moriba.skultem.domain.model.Enrollment.Status.ACTIVE || c == null || sec == null
                    || e.getStudent() == null || !levels.contains(c.getLevel())
                    || (classId != null && !classId.equals(c.getId()))
                    || (sectionId != null && !sectionId.equals(sec.getId()))
                    || (streamId != null && (e.getStream() == null || !streamId.equals(e.getStream().getId())))
                    || !seen.add(e.getId())) {
                continue;
            }
            added = true;
            all.add(new Object[] { c.getId(), c.getName(), sec.getId(), sec.getName(),
                    e.getStream() == null ? null : e.getStream().getId(),
                    e.getStream() == null ? null : e.getStream().getName(), e.getStudent().getGender(), 0L, 0L,
                    e.getId() });
        }
        if (!added) return rows;
        all.sort(java.util.Comparator
                .comparingInt((Object[] r) -> order.getOrDefault((String) r[0], Integer.MAX_VALUE))
                .thenComparing(r -> r[1] == null ? "" : (String) r[1])
                .thenComparingInt(r -> sectionOrder.getOrDefault((String) r[2], Integer.MAX_VALUE))
                .thenComparing(r -> r[3] == null ? "" : (String) r[3])
                .thenComparing(r -> r[5] == null ? "" : (String) r[5]));
        return all;
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
