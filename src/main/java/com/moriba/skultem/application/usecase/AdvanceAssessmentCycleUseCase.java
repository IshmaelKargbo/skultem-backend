package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AssessmentCycleAdvanceDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class AdvanceAssessmentCycleUseCase {

    private final TermRepository termRepository;
    private final ClassSubjectAssessmentLifeCycleRepository cycleRepository;
    private final AcademicYearRepository academicYearRepo;

    public AssessmentCycleAdvanceDTO execute(String schoolId, String termId) {
                var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                    .orElseThrow(() -> new NotFoundException("Active academic year not found"));

            var term = termRepository
                    .findActiveBySchoolAndAcademicYear(schoolId, academicYear.getId())
                    .orElseThrow(() -> new NotFoundException("Active term not found"));

            var cycles = cycleRepository.findAllBySchoolAndTerm(schoolId, termId);

            if (cycles.isEmpty()) {
                throw new RuleException("No assessment cycles found for this term");
            }

            int totalPositions = cycles.stream()
                    .map(item -> item.getAssessment().getPosition())
                    .max(Comparator.naturalOrder())
                    .orElse(0);

            var currentPosition = cycles.stream()
                    .filter(item -> (item.getStatus() != ClassSubjectAssessmentLifeCycle.Status.LOCKED && item.getStatus() != ClassSubjectAssessmentLifeCycle.Status.COMPLETED))
                    .map(item -> item.getAssessment().getPosition())
                    .min(Comparator.naturalOrder());

            if (currentPosition.isEmpty()) {
                return new AssessmentCycleAdvanceDTO(
                        termId,
                        totalPositions,
                        null,
                        totalPositions,
                        false,
                        true,
                        "All assessments are already completed for this term");
            }

            int position = currentPosition.get();

            var currentCycles = cycleRepository
                    .findAllBySchoolTermAndPosition(schoolId, termId, position);

            long pending = currentCycles.stream()
                    .filter(item -> (item.getStatus() != ClassSubjectAssessmentLifeCycle.Status.APPROVED && item.getStatus() != ClassSubjectAssessmentLifeCycle.Status.COMPLETED))
                    .count();

            if (pending > 0) {
                throw new RuleException(
                        "Cannot move to next assessment. " + pending + " class subject assessment(s) still pending approval");
            }

            currentCycles.forEach(ClassSubjectAssessmentLifeCycle::complete);

            var nextPosition = cycles.stream()
                    .map(item -> item.getAssessment().getPosition())
                    .filter(item -> item > position)
                    .min(Comparator.naturalOrder());

            List<ClassSubjectAssessmentLifeCycle> toSave = new ArrayList<>(currentCycles);

            if (nextPosition.isPresent()) {

                int next = nextPosition.get();

                var nextCycles = cycleRepository
                        .findAllBySchoolTermAndPosition(schoolId, termId, next);

                nextCycles.forEach(ClassSubjectAssessmentLifeCycle::markDraft);

                toSave.addAll(nextCycles);

                cycleRepository.saveAll(toSave);

                return new AssessmentCycleAdvanceDTO(
                        termId,
                        position,
                        next,
                        totalPositions,
                        true,
                        false,
                        "Assessment " + position + " closed. Assessment " + next + " is now active");
            }

            cycleRepository.saveAll(toSave);
            term.lock();
            termRepository.save(term);

            int nextTermNumber = term.getTermNumber() + 1;
            var nextTerm = termRepository
                    .findByTernNumberAndAcademicYearIdAndSchoolId(nextTermNumber, academicYear.getId(), schoolId);

            if (nextTerm.isPresent()) {

                var nt = nextTerm.get();
                nt.activate();
                termRepository.save(nt);

                return new AssessmentCycleAdvanceDTO(
                        termId,
                        position,
                        null,
                        totalPositions,
                        true,
                        true,
                        "Final assessment closed. Term completed. Term "
                                + nextTermNumber + " is now active");
            }

            return new AssessmentCycleAdvanceDTO(
                    termId,
                    position,
                    null,
                    totalPositions,
                    true,
                    true,
                    "Final assessment closed. Term assessment cycle completed");
    }
}