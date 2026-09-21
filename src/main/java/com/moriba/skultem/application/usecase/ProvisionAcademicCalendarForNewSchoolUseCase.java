package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.NationalCalendarRepository;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Gives a brand-new school the current platform-wide academic year and its terms (see
 * {@link com.moriba.skultem.domain.model.NationalAcademicYear}) so it doesn't have to build its
 * calendar by hand before it can do anything.
 * <p>
 * The school gets copies - the year (made active, like the first year a school creates itself) and
 * one term per national term. Whichever term is running today is activated, and any term that
 * already ended is closed; if today falls in a break, the next term to start is the one activated.
 * Activating it is what seeds the school's platform fee (see
 * {@link SeedPlatformFeeForAcademicYearUseCase}), so the fee is in place from day one. If every
 * term has already ended the national calendar is stale: the terms are created closed and nothing
 * is activated, leaving the school to configure its next year.
 * <p>
 * A no-op when no national calendar is configured or the school already has an active academic
 * year, so it's safe to call more than once.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ProvisionAcademicCalendarForNewSchoolUseCase {

    private final NationalCalendarRepository nationalRepo;
    private final AcademicYearRepository academicYearRepo;
    private final TermRepository termRepo;
    private final ReferenceGeneratorUsecase rg;
    private final SeedPlatformFeeForAcademicYearUseCase seedPlatformFeeForAcademicYearUseCase;

    public void execute(String schoolId) {
        var national = nationalRepo.findCurrent().orElse(null);
        if (national == null || national.getTerms().isEmpty()) {
            return;
        }
        if (academicYearRepo.findActiveBySchool(schoolId).isPresent()) {
            return;
        }

        var year = AcademicYear.create(UUID.randomUUID().toString(), schoolId, national.getName(),
                national.getStartDate(), national.getEndDate());
        year.setActive(true);
        academicYearRepo.save(year);

        var today = LocalDate.now();
        // First term that hasn't ended yet: the one in progress, or the next one during a break.
        Integer activeNumber = national.getTerms().stream()
                .filter(t -> !t.endDate().isBefore(today))
                .map(t -> t.termNumber())
                .findFirst()
                .orElse(null);

        Term activated = null;
        for (var nt : national.getTerms()) {
            Term.Status status;
            if (activeNumber == null || nt.termNumber() < activeNumber) {
                status = Term.Status.CLOSED;
            } else if (nt.termNumber() == activeNumber) {
                status = Term.Status.ACTIVE;
            } else {
                status = Term.Status.UPCOMING;
            }

            var term = Term.create(rg.generate("TERM", "TRM"), schoolId, year, nt.name(), nt.termNumber(), status,
                    nt.startDate(), nt.endDate());
            termRepo.save(term);

            if (status == Term.Status.ACTIVE) {
                activated = term;
            }
        }

        if (activated != null) {
            seedPlatformFeeForAcademicYearUseCase.execute(schoolId, year, activated);
        }
    }
}
