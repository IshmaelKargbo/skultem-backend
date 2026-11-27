package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.application.services.SchoolClosedDays;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.service.SchoolDays;

import lombok.RequiredArgsConstructor;

// "The term so far": a term (the given one, or the year's active term) from its start up to today -
// or to its end once it's over - and the school days in that window. Shared by the term-to-date
// attendance figures so they all count the same days.
@Service
@RequiredArgsConstructor
public class ResolveTermSchoolDaysUseCase {

    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;
    private final TermRepository termRepo;
    private final com.moriba.skultem.domain.repository.AcademicYearRepository academicYearRepo;
    private final SchoolClosedDays closedDays;

    public record TermWindow(AcademicYear academicYear, Term term, LocalDate asOf, List<LocalDate> schoolDays) {
    }

    // School days: every weekday from the term's start date up to today (or the term's end, once it is over) that
    // isn't a day the school is closed (a holiday, or a calendar entry marked no school) - whether or not attendance was taken that day.
    public TermWindow execute(String schoolId, String academicYearId, String termId) {
        return calendarDays(schoolId, academicYearId, termId);
    }

    public TermWindow calendarDays(String schoolId, String academicYearId, String termId) {
        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

        Term term = (termId != null && !termId.isBlank())
                ? termRepo.findByIdAndAcademicYearIdAndSchoolId(termId, academicYear.getId(), schoolId)
                        .orElseThrow(() -> new NotFoundException("Term not found"))
                : termRepo.findActiveBySchoolAndAcademicYear(schoolId, academicYear.getId())
                        .orElseThrow(() -> new NotFoundException("No active term for this academic year"));

        LocalDate today = LocalDate.now();
        LocalDate asOf = term.getEndDate() != null && today.isAfter(term.getEndDate()) ? term.getEndDate() : today;

        var holidays = closedDays.forYear(schoolId, academicYear.getId());

        var schoolDays = term.getStartDate() == null ? List.<LocalDate>of()
                : SchoolDays.between(term.getStartDate(), asOf, holidays);

        return new TermWindow(academicYear, term, asOf, schoolDays);
    }

    // between() for the active academic year; none when the school has no active year.
    public List<LocalDate> inActiveYear(String schoolId, LocalDate from, LocalDate to) {
        return academicYearRepo.findActiveBySchool(schoolId)
                .map(year -> between(schoolId, year.getId(), from, to))
                .orElse(List.of());
    }

    // School days from `from` to `to` (both inclusive), cut off at today: weekdays that fall inside one of the
    // year's terms and aren't a day the school is closed. The summary reports (a month, a term, a range) count
    // against these, so they agree with the students list.
    public List<LocalDate> between(String schoolId, String academicYearId, LocalDate from, LocalDate to) {
        if (from == null || to == null) return List.of();
        LocalDate today = LocalDate.now();
        LocalDate end = to.isAfter(today) ? today : to;
        var closed = closedDays.forYear(schoolId, academicYearId);
        var days = new java.util.TreeSet<LocalDate>();
        for (Term t : termRepo.findByAcademicYearIdAndSchool(academicYearId, schoolId)) {
            if (t.getStartDate() == null || t.getEndDate() == null) continue;
            LocalDate s = t.getStartDate().isAfter(from) ? t.getStartDate() : from;
            LocalDate e = t.getEndDate().isBefore(end) ? t.getEndDate() : end;
            if (!e.isBefore(s)) days.addAll(SchoolDays.between(s, e, closed));
        }
        return List.copyOf(days);
    }
}
