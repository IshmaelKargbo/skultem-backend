package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
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
// Every student is measured over the whole term from its first day, whenever their enrollment was
// created - schools onboarded mid-term enroll everyone late and then backfill the earlier registers.
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

        // A student can have more than one enrollment in the year (a class change): their attendance
        // across all of them adds up.
        Map<String, Long> present = new LinkedHashMap<>();
        if (!window.schoolDays().isEmpty()) {
            for (Object[] row : attendanceRepo.presentCountsByStudent(schoolId, window.academicYear().getId(), classId,
                    sectionScopeService.levels(), term.getStartDate(), window.asOf())) {
                present.merge((String) row[0], row[2] == null ? 0L : ((Number) row[2]).longValue(), Long::sum);
            }
        }

        LocalDate from = term.getStartDate();
        int days = window.schoolDays().size();
        List<StudentRow> students = present.entrySet().stream().map(entry -> {
            // Capped: a record on a day later declared a holiday can't push anyone past 100%.
            long attended = Math.min(entry.getValue(), days);
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

        long present = 0, late = 0, excused = 0, recorded = 0;
        if (!window.schoolDays().isEmpty()) {
            for (Object[] r : attendanceRepo.statusCountsForStudent(schoolId, studentId, window.academicYear().getId(),
                    term.getStartDate(), window.asOf())) {
                present += r[1] == null ? 0 : ((Number) r[1]).longValue();
                late += r[2] == null ? 0 : ((Number) r[2]).longValue();
                excused += r[3] == null ? 0 : ((Number) r[3]).longValue();
                recorded += r[4] == null ? 0 : ((Number) r[4]).longValue();
            }
        }

        LocalDate from = term.getStartDate();
        int days = window.schoolDays().size();

        // Capped like execute(): records outside the counted days can't overflow it.
        recorded = Math.min(recorded, days);
        long attended = Math.min(present + late, days);
        long absent = Math.max(0, recorded - present - late - excused);
        long notMarked = Math.max(0, days - recorded);

        return new StudentTermAttendanceDetailDTO(term.getId(), term.getName(), term.getStartDate(), window.asOf(),
                from, days, present, late, absent, excused, notMarked,
                days == 0 ? null : AttendanceRateCalculator.rate(attended, days));
    }
}
