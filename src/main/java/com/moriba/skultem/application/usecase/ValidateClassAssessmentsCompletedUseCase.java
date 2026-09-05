package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * A class can only be promoted once every assessment cycle for its subjects, across every term of the
 * academic year being promoted from, has been completed (COMPLETED) or closed out (LOCKED).
 */
@Service
// See ValidateNextAcademicYearUseCase for why RuleException must not mark this transaction
// rollback-only - callers like ListMyClassMasterAssignmentsUseCase catch it and keep going.
@Transactional(dontRollbackOn = RuleException.class)
@RequiredArgsConstructor
public class ValidateClassAssessmentsCompletedUseCase {

    private final TermRepository termRepository;
    private final ClassSubjectAssessmentLifeCycleRepository cycleRepository;

    public void execute(String schoolId, String classId, String academicYearId) {
        var terms = termRepository.findByAcademicYearIdAndSchool(academicYearId, schoolId);

        for (var term : terms) {
            var cycles = cycleRepository.findAllBySchoolAndTermAndClass(schoolId, term.getId(), classId);

            boolean pending = cycles.stream().anyMatch(cycle -> cycle.getStatus() != ClassSubjectAssessmentLifeCycle.Status.LOCKED
                    && cycle.getStatus() != ClassSubjectAssessmentLifeCycle.Status.COMPLETED);

            if (pending) {
                throw new RuleException(
                        "Promotion is allowed only after all assessments for this class are completed");
            }
        }
    }
}
