package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.HolidayRepository;
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
    private final HolidayRepository holidayRepo;

    public record TermWindow(AcademicYear academicYear, Term term, LocalDate asOf, List<LocalDate> schoolDays) {
    }

    public TermWindow execute(String schoolId, String academicYearId, String termId) {
        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

        Term term = (termId != null && !termId.isBlank())
                ? termRepo.findByIdAndAcademicYearIdAndSchoolId(termId, academicYear.getId(), schoolId)
                        .orElseThrow(() -> new NotFoundException("Term not found"))
                : termRepo.findActiveBySchoolAndAcademicYear(schoolId, academicYear.getId())
                        .orElseThrow(() -> new NotFoundException("No active term for this academic year"));

        LocalDate today = LocalDate.now();
        LocalDate asOf = term.getEndDate() != null && today.isAfter(term.getEndDate()) ? term.getEndDate() : today;

        var holidays = holidayRepo.findAllBySchoolIdAndAcademicYear(schoolId, academicYear.getId(), Pageable.unpaged())
                .getContent().stream().map(h -> h.getDate()).toList();

        var schoolDays = term.getStartDate() == null ? List.<LocalDate>of()
                : SchoolDays.between(term.getStartDate(), asOf, holidays);

        return new TermWindow(academicYear, term, asOf, schoolDays);
    }
}
