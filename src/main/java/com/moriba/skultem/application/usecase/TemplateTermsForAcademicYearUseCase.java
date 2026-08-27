package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Copies every term from one academic year into another, shifted a year forward and clamped to the
 * target year's own date range. Used whenever a "next year" link is established - both when
 * {@link ConfigureNextAcademicYearUseCase} creates that year outright, and when
 * {@link AssignNextAcademicYearUseCase} links one the school already created by hand - so a school
 * always ends up with next year's terms ready to go, regardless of which path they used.
 * <p>
 * Only backfills term numbers the target year doesn't already have - a term number an admin (or an
 * earlier partial run) already set up by hand isn't overridden, but that no longer blocks the
 * remaining term numbers from being templated too.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class TemplateTermsForAcademicYearUseCase {

    private final TermRepository termRepository;
    private final ReferenceGeneratorUsecase rg;

    public List<Term> execute(String schoolId, AcademicYear sourceYear, AcademicYear targetYear) {
        Set<Integer> existingTermNumbers = termRepository.findByAcademicYearIdAndSchool(targetYear.getId(), schoolId)
                .stream()
                .map(Term::getTermNumber)
                .collect(Collectors.toSet());

        var sourceTerms = termRepository.findByAcademicYearIdAndSchool(sourceYear.getId(), schoolId).stream()
                .sorted(Comparator.comparingInt(Term::getTermNumber))
                .toList();

        List<Term> createdTerms = new ArrayList<>();

        for (var sourceTerm : sourceTerms) {
            if (existingTermNumbers.contains(sourceTerm.getTermNumber())) {
                continue;
            }

            var termId = rg.generate("TERM", "TRM");

            var termStart = sourceTerm.getStartDate().plusYears(1);
            var termEnd = sourceTerm.getEndDate().plusYears(1);

            if (termStart.isBefore(targetYear.getStartDate())) {
                termStart = targetYear.getStartDate();
            }
            if (termEnd.isAfter(targetYear.getEndDate())) {
                termEnd = targetYear.getEndDate();
            }

            var term = Term.create(termId, schoolId, targetYear, sourceTerm.getName(), sourceTerm.getTermNumber(),
                    Term.Status.UPCOMING, termStart, termEnd);
            termRepository.save(term);
            createdTerms.add(term);
        }

        return createdTerms;
    }
}
