package com.moriba.skultem.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentTermAttendanceDTO;
import com.moriba.skultem.application.dto.StudentTermAttendanceDTO.StudentRow;
import com.moriba.skultem.application.dto.StudentTermAttendanceDetailDTO;
import com.moriba.skultem.application.services.SectionScopeService;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.service.AttendanceRateCalculator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Term-to-date attendance over every school day so far: the students list and class roster columns
// (execute) and one student's breakdown on their profile / the parent view (executeForStudent).
// A student only owes the school days since they enrolled - someone who joined in week 6 is measured
// from week 6, not from the first day of term.
@Service
@Transactional
@RequiredArgsConstructor
public class GetStudentTermAttendanceUseCase {

    private final ResolveTermSchoolDaysUseCase resolveTermSchoolDaysUseCase;
    private final AttendanceRepository attendanceRepo;
    private final SectionScopeService sectionScopeService;

    // classId: optional, narrows the list to one class (the class page roster).
    public StudentTermAttendanceDTO execute(String schoolId, String academicYearId, String termId, String classId) {
        var window = resolveTermSchoolDaysUseCase.execute(schoolId, academicYearId, termId);
        var term = window.term();

        // A student can have more than one enrollment in the year (a class change): their days run from
        // the earliest one, and their attendance across all of them adds up.
        Map<String, Instant> enrolledAt = new LinkedHashMap<>();
        Map<String, Long> present = new LinkedHashMap<>();
        if (!window.schoolDays().isEmpty()) {
            for (Object[] row : attendanceRepo.presentCountsByStudent(schoolId, window.academicYear().getId(), classId,
                    sectionScopeService.levels(), term.getStartDate(), window.asOf())) {
                String studentId = (String) row[0];
                enrolledAt.merge(studentId, (Instant) row[1], (a, b) -> a.isBefore(b) ? a : b);
                present.merge(studentId, row[2] == null ? 0L : ((Number) row[2]).longValue(), Long::sum);
            }
        }

        List<StudentRow> students = enrolledAt.entrySet().stream().map(entry -> {
            LocalDate from = countedFrom(term.getStartDate(), entry.getValue());
            int days = daysFrom(window.schoolDays(), from);
            // Capped: a record on a day later declared a holiday can't push anyone past 100%.
            long attended = Math.min(present.get(entry.getKey()), days);
            return new StudentRow(entry.getKey(), from, days, attended,
                    days == 0 ? null : AttendanceRateCalculator.rate(attended, days));
        }).toList();

        return new StudentTermAttendanceDTO(term.getId(), term.getName(), term.getStartDate(), window.asOf(),
                window.schoolDays().size(), students);
    }

    public StudentTermAttendanceDetailDTO executeForStudent(String schoolId, String studentId, String academicYearId,
            String termId) {
        var window = resolveTermSchoolDaysUseCase.execute(schoolId, academicYearId, termId);
        var term = window.term();

        Instant enrolledAt = null;
        long present = 0, late = 0, excused = 0, recorded = 0;
        if (!window.schoolDays().isEmpty()) {
            for (Object[] r : attendanceRepo.statusCountsForStudent(schoolId, studentId, window.academicYear().getId(),
                    term.getStartDate(), window.asOf())) {
                Instant at = (Instant) r[0];
                if (at != null && (enrolledAt == null || at.isBefore(enrolledAt))) enrolledAt = at;
                present += r[1] == null ? 0 : ((Number) r[1]).longValue();
                late += r[2] == null ? 0 : ((Number) r[2]).longValue();
                excused += r[3] == null ? 0 : ((Number) r[3]).longValue();
                recorded += r[4] == null ? 0 : ((Number) r[4]).longValue();
            }
        }

        LocalDate from = enrolledAt == null ? term.getStartDate() : countedFrom(term.getStartDate(), enrolledAt);
        int days = daysFrom(window.schoolDays(), from);

        // Capped like execute(): records outside the counted days can't overflow it.
        recorded = Math.min(recorded, days);
        long attended = Math.min(present + late, days);
        long absent = Math.max(0, recorded - present - late - excused);
        long notMarked = Math.max(0, days - recorded);

        return new StudentTermAttendanceDetailDTO(term.getId(), term.getName(), term.getStartDate(), window.asOf(),
                from, days, present, late, absent, excused, notMarked,
                days == 0 ? null : AttendanceRateCalculator.rate(attended, days));
    }

    // The later of the term's start and the day they enrolled (in the server's zone, like the rest of the
    // app's createdAt dates).
    private static LocalDate countedFrom(LocalDate termStart, Instant enrolledAt) {
        if (enrolledAt == null) return termStart;
        LocalDate enrolled = enrolledAt.atZone(ZoneId.systemDefault()).toLocalDate();
        return termStart == null || enrolled.isAfter(termStart) ? enrolled : termStart;
    }

    private static int daysFrom(List<LocalDate> schoolDays, LocalDate from) {
        return (int) schoolDays.stream().filter(d -> from == null || !d.isBefore(from)).count();
    }
}
