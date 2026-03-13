package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TermDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.TermMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ActivateTermUseCase {

    private final TermRepository termRepository;
    private final ClassSubjectAssessmentLifeCycleRepository cycleRepository;

    @AuditLogAnnotation(action = "TERM_ACTIVATED")
    public TermDTO execute(String schoolId, String termId) {
        var target = termRepository.findByIdAndSchoolId(termId, schoolId)
                .orElseThrow(() -> new NotFoundException("Term not found"));

        if (target.getStatus() == Term.Status.CLOSED) {
            throw new RuleException("Closed term cannot be activated");
        }

        var allTerms = termRepository.findAllBySchoolId(schoolId, Pageable.unpaged()).getContent();
        var currentActiveTerm = allTerms.stream()
                .filter(term -> term.getStatus() == Term.Status.ACTIVE)
                .findFirst()
                .orElse(null);

        if (currentActiveTerm != null && !currentActiveTerm.getId().equals(target.getId())) {
            if (!isTermAssessmentCompleted(schoolId, currentActiveTerm.getId())) {
                throw new RuleException(
                        "Cannot activate the next term until all assessments in " + currentActiveTerm.getName()
                                + " are completed");
            }

            if (target.getTermNumber() != currentActiveTerm.getTermNumber() + 1) {
                throw new RuleException("You can only activate the immediate next term");
            }

            currentActiveTerm.lock();
            termRepository.save(currentActiveTerm);
        }

        allTerms.forEach(term -> {
            if (term.getId().equals(termId)) {
                term.activate();
            } else if (term.getStatus() != Term.Status.CLOSED) {
                term.markUpcoming();
            }
            termRepository.save(term);
        });

        var targetCycles = cycleRepository.findAllBySchoolAndTerm(schoolId, termId);
        if (!targetCycles.isEmpty()) {
            boolean hasOpenCycle = targetCycles.stream()
                    .anyMatch(cycle -> cycle.getStatus() != ClassSubjectAssessmentLifeCycle.Status.LOCKED);

            if (!hasOpenCycle) {
                int firstPosition = targetCycles.stream()
                        .map(cycle -> cycle.getAssessment().getPosition())
                        .min(Integer::compareTo)
                        .orElse(1);

                var firstCycles = cycleRepository.findAllBySchoolTermAndPosition(schoolId, termId, firstPosition);
                firstCycles.forEach(ClassSubjectAssessmentLifeCycle::markDraft);
                cycleRepository.saveAll(firstCycles);
            }
        }

        return TermMapper.toDTO(target);
    }

    private boolean isTermAssessmentCompleted(String schoolId, String termId) {
        var cycles = cycleRepository.findAllBySchoolAndTerm(schoolId, termId);
        if (cycles.isEmpty()) {
            return false;
        }

        return cycles.stream()
                .allMatch(cycle -> cycle.getStatus() == ClassSubjectAssessmentLifeCycle.Status.LOCKED);
    }
}
