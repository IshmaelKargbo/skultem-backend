package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ValidateAcademicYearTermsUseCase {

    private final TermRepository termRepository;

    public void execute(String schoolId, String academicYearId) {
        var terms = termRepository.findByAcademicYearIdAndSchool(academicYearId, schoolId);
        if (terms.size() < 3) {
            throw new RuleException("Promotion is allowed only after all 3 terms are configured and completed");
        }

        boolean allTermsCompleted = terms.stream()
                .allMatch(term -> term.getStatus() == Term.Status.CLOSED);

        if (!allTermsCompleted) {
            throw new RuleException("Promotion is allowed only after all 3 terms are completed");
        }
    }
}
