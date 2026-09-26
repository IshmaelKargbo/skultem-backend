package com.moriba.skultem.application.usecase;

import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.AssessmentStructureService;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.AssessmentCaEntry;
import com.moriba.skultem.domain.repository.AssessmentCaEntryRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;
import com.moriba.skultem.domain.repository.StudentAssessmentRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Continuous assessment goes in two steps: the CA recordings first, then the formal test. This closes the first
// step - every one of the assessment's recordings ("Week 1" to "Week 6", however many it was opened with) has to be
// in for every student - and only then can the formal test be entered.
@Service
@Transactional
@RequiredArgsConstructor
public class SubmitContinuousCaUseCase {

    private final TeacherSubjectRepository teacherSubjectRepo;
    private final StudentAssessmentRepository studentAssessmentRepo;
    private final AssessmentScoreRepository assessmentScoreRepo;
    private final AssessmentCaEntryRepository caEntryRepo;
    private final ClassSubjectAssessmentLifeCycleRepository cycleRepo;
    private final AssessmentStructureService structureService;

    @AuditLogAnnotation(action = "CONTINUOUS_ASSESSMENT_CA_SUBMITTED")
    public void execute(String schoolId, String teacherSubjectId, String assessmentId, String termId) {
        var ts = teacherSubjectRepo.findByIdAndSchoolId(teacherSubjectId, schoolId)
                .orElseThrow(() -> new NotFoundException("Teacher subject not found"));
        var session = ts.getSession();

        var studentAssessments = studentAssessmentRepo.findAllBySubjectAndSessionAndTermId(ts.getSubject().getId(),
                session.getId(), termId);
        if (studentAssessments.isEmpty()) {
            throw new NotFoundException("No student assessments found");
        }

        var scores = new java.util.ArrayList<com.moriba.skultem.domain.model.AssessmentScore>();
        for (var sa : studentAssessments) {
            scores.addAll(assessmentScoreRepo.findAllByStudentAssessmentIdAndAssessmentId(sa.getId(), assessmentId));
        }
        if (scores.isEmpty()) {
            throw new NotFoundException("Assessment not found for these students");
        }

        var cycles = scores.stream().map(s -> s.getCycle()).distinct().toList();
        cycleRepo.saveAll(structureService.freezeOpened(cycles));
        var cycle = cycles.get(0);

        var recorded = caEntryRepo.findAllByScoreIds(scores.stream().map(s -> s.getId()).toList()).stream()
                .collect(Collectors.groupingBy(AssessmentCaEntry::getAssessmentScoreId,
                        Collectors.mapping(AssessmentCaEntry::getEntryNumber, Collectors.toSet())));
        int expected = cycle.getCaEntries() == null ? 0 : cycle.getCaEntries();
        for (var score : scores) {
            int have = recorded.getOrDefault(score.getId(), java.util.Set.of()).size();
            if (have < expected) {
                throw new RuleException("Every student needs all " + expected + " CA recordings before the CA can be "
                        + "submitted - at least one still has " + (expected - have) + " missing");
            }
        }

        cycle.submitCa();
        cycleRepo.save(cycle);
    }
}
