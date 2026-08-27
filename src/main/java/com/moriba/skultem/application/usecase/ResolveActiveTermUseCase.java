package com.moriba.skultem.application.usecase;

import java.util.Comparator;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * The term to treat as "active" for a school right now: the one actually marked ACTIVE, or - once
 * every term in the active academic year has been closed out (the final assessment cycle closes
 * its term without a next one to open within the year) - the last one, so term-scoped views like
 * the assessment cycle still have something to show instead of erroring until the next academic
 * year is activated.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ResolveActiveTermUseCase {

    private final TermRepository termRepo;
    private final AcademicYearRepository academicYearRepo;

    public Optional<Term> execute(String schoolId) {
        return execute(schoolId, null);
    }

    /**
     * @param academicYearId when given, resolves "active term" against that year specifically
     *                       instead of the school's globally active one - lets a user browse a
     *                       past year's data without touching what's active for everyone else.
     */
    public Optional<Term> execute(String schoolId, String academicYearId) {
        var academicYear = (academicYearId != null && !academicYearId.isBlank())
                ? academicYearRepo.findByIdAndSchoolId(academicYearId, schoolId).orElse(null)
                : academicYearRepo.findActiveBySchool(schoolId).orElse(null);

        if (academicYear == null) {
            return Optional.empty();
        }

        var active = termRepo.findActiveBySchoolAndAcademicYear(schoolId, academicYear.getId());
        if (active.isPresent()) {
            return active;
        }

        var terms = termRepo.findByAcademicYearIdAndSchool(academicYear.getId(), schoolId);
        boolean allClosed = !terms.isEmpty() && terms.stream().allMatch(t -> t.getStatus() == Term.Status.CLOSED);

        return allClosed
                ? terms.stream().max(Comparator.comparingInt(Term::getTermNumber))
                : Optional.empty();
    }
}
