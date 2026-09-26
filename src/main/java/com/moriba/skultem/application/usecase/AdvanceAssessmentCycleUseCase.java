package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AssessmentCycleAdvanceDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
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
        private final com.moriba.skultem.application.services.AssessmentStructureService structureService;

        private final com.moriba.skultem.domain.repository.SchoolRepository schoolRepo;
        private final com.moriba.skultem.domain.repository.ManagementSectionRepository sectionRepo;

        // Management sections run their assessments separately - Primary can be on Test 2 while Secondary is still on
        // Test 1 - so a section advances on its own. In a school without sections (sectionId null) it is the whole
        // school, as before. The term closes, and the next one opens, only once EVERY section has finished its last
        // assessment (the term is one shared calendar period).
        @AuditLogAnnotation(action = "ASSESSMENT_ADVANCED")
        public AssessmentCycleAdvanceDTO execute(String schoolId, String termId, String requestedSectionId) {

                var school = schoolRepo.findById(schoolId).orElseThrow(() -> new NotFoundException("School not found"));
                boolean sectionBased = school.getManagementModel() == com.moriba.skultem.domain.model.School.ManagementModel.SECTION_BASED;
                String sectionId = sectionBased ? requestedSectionId : null;
                String sectionName = null;
                java.util.Set<com.moriba.skultem.domain.vo.Level> levels = null;
                if (sectionBased) {
                        if (sectionId == null || sectionId.isBlank()) {
                                throw new RuleException("Choose which section to move - each section runs its "
                                                + "assessments separately");
                        }
                        var section = sectionRepo.findBySchoolId(schoolId).stream()
                                        .filter(x -> x.getId().equals(sectionId)).findFirst()
                                        .orElseThrow(() -> new NotFoundException("Management section not found"));
                        sectionName = section.getName();
                        levels = structureService.levelsOfSection(schoolId, sectionId);
                }
                final var sectionLevels = levels;
                final String sectionLabel = sectionName;

                var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                                .orElseThrow(() -> new NotFoundException("Active academic year not found"));

                var term = termRepository
                                .findByIdAndAcademicYearIdAndSchoolId(termId, academicYear.getId(), schoolId)
                                .orElseThrow(() -> new NotFoundException(
                                                "Term not found for this school and academic year"));

                var termCycles = cycleRepository.findAllBySchoolAndTerm(schoolId, termId);
                // This section's cycles - the ones this advance moves.
                var cycles = termCycles.stream()
                                .filter(c -> sectionLevels == null || sectionLevels.contains(
                                                c.getSubject().getSession().getClazz().getLevel()))
                                .toList();

                if (cycles.isEmpty()) {
                        throw new RuleException("No assessment cycles found for "
                                        + (sectionLabel == null ? "this term" : sectionLabel + " in this term"));
                }

                int totalPositions = cycles.stream()
                                .map(item -> item.getAssessment().getPosition())
                                .max(Comparator.naturalOrder())
                                .orElse(0);

                var currentPosition = cycles.stream()
                                .filter(item -> item.getStatus() != ClassSubjectAssessmentLifeCycle.Status.LOCKED
                                                && item.getStatus() != ClassSubjectAssessmentLifeCycle.Status.COMPLETED)
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
                                        "All assessments are already completed for "
                                                        + (sectionLabel == null ? "this term" : sectionLabel + " this term"),
                                        sectionId, sectionLabel, true);
                }

                int position = currentPosition.get();

                var currentCycles = cycles.stream()
                                .filter(item -> item.getAssessment().getPosition() == position)
                                .toList();

                var pending = currentCycles.stream()
                                .filter(item -> item.getStatus() != ClassSubjectAssessmentLifeCycle.Status.APPROVED
                                                && item.getStatus() != ClassSubjectAssessmentLifeCycle.Status.COMPLETED)
                                .toList();

                if (!pending.isEmpty()) {
                        // Say exactly which ones - "3 pending" leaves an admin hunting through every class.
                        var names = pending.stream().limit(5)
                                        .map(c -> c.getSubject().getSession().getClazz().getName() + " " + c.getSubject().getSubject().getName()
                                                        + " (" + c.getAssessment().getName() + " - "
                                                        + c.getStatus().name().toLowerCase().replace('_', ' ') + ")")
                                        .collect(java.util.stream.Collectors.joining(", "));
                        throw new RuleException(
                                        "Cannot advance assessment" + (sectionLabel == null ? "" : " for " + sectionLabel)
                                                        + ". " + pending.size()
                                                        + " class subject assessment(s) still pending approval: " + names
                                                        + (pending.size() > 5 ? " and " + (pending.size() - 5) + " more" : "")
                                                        + ". Approve or finish them first.");
                }

                currentCycles.forEach(ClassSubjectAssessmentLifeCycle::complete);

                List<ClassSubjectAssessmentLifeCycle> toSave = new ArrayList<>(currentCycles);

                var nextPosition = cycles.stream()
                                .map(item -> item.getAssessment().getPosition())
                                .filter(p -> p > position)
                                .min(Comparator.naturalOrder());

                if (nextPosition.isPresent()) {
                        int next = nextPosition.get();

                        var nextCycles = cycles.stream()
                                        .filter(item -> item.getAssessment().getPosition() == next)
                                        .toList();

                        nextCycles.forEach(ClassSubjectAssessmentLifeCycle::markDraft);
                        // The next assessment opens now, so it takes the configuration in force now - earlier ones keep theirs.
                        structureService.freezeOpened(nextCycles);
                        toSave.addAll(nextCycles);
                        cycleRepository.saveAll(toSave);

                        return new AssessmentCycleAdvanceDTO(
                                        termId,
                                        position,
                                        next,
                                        totalPositions,
                                        true,
                                        false,
                                        (sectionLabel == null ? "" : sectionLabel + ": ") + "Assessment " + position
                                                        + " closed. Assessment " + next + " is now active.",
                                        sectionId, sectionLabel, false);
                }

                cycleRepository.saveAll(toSave);

                // This section is through its last assessment. The term only closes when every section is.
                boolean everyoneDone = termCycles.stream().allMatch(
                                c -> c.getStatus() == ClassSubjectAssessmentLifeCycle.Status.COMPLETED
                                                || currentCycles.contains(c));
                if (!everyoneDone) {
                        return new AssessmentCycleAdvanceDTO(
                                        termId,
                                        position,
                                        null,
                                        totalPositions,
                                        true,
                                        false,
                                        (sectionLabel == null ? "" : sectionLabel + ": ") + "Final assessment closed. "
                                                        + "The term stays open until the other sections finish theirs.",
                                        sectionId, sectionLabel, true);
                }

                term.lock();
                termRepository.save(term);

                int nextTermNumber = term.getTermNumber() + 1;
                var nextTerm = termRepository
                                .findByTernNumberAndAcademicYearIdAndSchoolId(nextTermNumber, academicYear.getId(),
                                                schoolId);

                if (nextTerm.isPresent()) {
                        var nt = nextTerm.get();
                        nt.activate();
                        termRepository.save(nt);

                        var nextTermCycles = cycleRepository.findAllBySchoolAndTerm(schoolId, nt.getId());

                        if (!nextTermCycles.isEmpty()) {
                                // Every section starts the new term on its own first assessment.
                                var byLevel = nextTermCycles.stream().collect(java.util.stream.Collectors.groupingBy(
                                                c -> c.getSubject().getSession().getClazz().getLevel()));
                                var opened = new ArrayList<ClassSubjectAssessmentLifeCycle>();
                                for (var group : byLevel.values()) {
                                        int firstPosition = group.stream()
                                                        .map(item -> item.getAssessment().getPosition())
                                                        .min(Comparator.naturalOrder())
                                                        .orElse(1);
                                        group.stream().filter(c -> c.getAssessment().getPosition() == firstPosition)
                                                        .forEach(opened::add);
                                }
                                opened.forEach(ClassSubjectAssessmentLifeCycle::markDraft);
                                structureService.freezeOpened(opened);
                                cycleRepository.saveAll(opened);
                        }

                        return new AssessmentCycleAdvanceDTO(
                                        termId,
                                        position,
                                        null,
                                        totalPositions,
                                        true,
                                        true,
                                        "Final assessment closed for every section. Term " + term.getTermNumber()
                                                        + " completed. Term " + nextTermNumber + " is now active.",
                                        sectionId, sectionLabel, true);
                }

                return new AssessmentCycleAdvanceDTO(
                                termId,
                                position,
                                null,
                                totalPositions,
                                true,
                                true,
                                "Final assessment closed for every section. All terms for this academic year are completed.",
                                sectionId, sectionLabel, true);
        }
}
