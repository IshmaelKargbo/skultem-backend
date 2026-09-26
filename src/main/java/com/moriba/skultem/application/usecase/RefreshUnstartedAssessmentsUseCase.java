package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.services.AssessmentStructureService;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.repository.AssessmentCaEntryRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Freezing only ever happens once, so a section that turns on Continuous Assessment sees nothing change
// for any assessment already sitting open - by design (see AssessmentStructureService.freezeOpened). That
// is exactly right for an assessment someone has started grading, but it also strands every assessment
// that is genuinely still blank: nobody can use the new setup until a fresh term or a fresh enrollment
// happens to create one. This lets an admin bring the section's still-untouched assessments onto the
// configuration immediately, without ever moving one that has so much as one recorded score.
@Service
@Transactional
@RequiredArgsConstructor
public class RefreshUnstartedAssessmentsUseCase {

    public record Result(int refreshed, int skipped) {
        @Override
        public String toString() {
            return refreshed + " assessment(s) moved onto the current setup, " + skipped
                    + " left alone because they already have grades";
        }
    }

    private final ClassSubjectAssessmentLifeCycleRepository cycleRepo;
    private final AssessmentScoreRepository scoreRepo;
    private final AssessmentCaEntryRepository caEntryRepo;
    private final AssessmentStructureService structureService;

    @AuditLogAnnotation(action = "ASSESSMENT_CONFIGURATION_APPLIED_TO_UNSTARTED")
    public Result execute(String schoolId, String sectionId) {
        var levels = structureService.levelsOfSection(schoolId, sectionId);
        var candidates = cycleRepo.findAllOpenBySchool(schoolId).stream()
                .filter(cycle -> cycle.isStructureFrozen())
                // A term that has closed is history - its (empty) assessments are never rewritten.
                .filter(cycle -> cycle.getTerm() == null || cycle.getTerm().getStatus() != com.moriba.skultem.domain.model.Term.Status.CLOSED)
                .filter(cycle -> levels == null || levels.contains(cycle.getSubject().getSession().getClazz().getLevel()))
                .toList();

        var toSave = new ArrayList<com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle>();
        int skipped = 0;
        for (var cycle : candidates) {
            var scores = scoreRepo.findAllByCycle(cycle.getId());
            if (!isUntouched(scores)) {
                skipped++;
                continue;
            }
            var config = structureService.forLevel(schoolId, cycle.getSubject().getSession().getClazz().getLevel());
            if (cycle.resyncStructure(config)) {
                toSave.add(cycle);
            }
        }

        if (!toSave.isEmpty()) {
            cycleRepo.saveAll(toSave);
        }
        return new Result(toSave.size(), skipped);
    }

    // Nobody has graded a single one of these scores yet - not through the simple grid, not through a CA
    // recording. gradedByUserId is the same attribution flag either path sets on first edit.
    private boolean isUntouched(List<AssessmentScore> scores) {
        if (scores.isEmpty()) {
            return true;
        }
        if (scores.stream().anyMatch(s -> s.getGradedByUserId() != null)) {
            return false;
        }
        var scoreIds = scores.stream().map(AssessmentScore::getId).toList();
        return caEntryRepo.findAllByScoreIds(scoreIds).isEmpty();
    }
}
