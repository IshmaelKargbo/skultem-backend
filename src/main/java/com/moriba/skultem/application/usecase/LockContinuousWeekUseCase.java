package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.HashSet;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.AssessmentStructureService;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.repository.AssessmentCaEntryRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;
import com.moriba.skultem.domain.repository.StudentAssessmentRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Locks a completed CA recording ("Week 3") so nobody quietly changes it afterwards - a week can only be locked
// once every student has a mark for it. Locking is what makes the record trustworthy: after this a teacher can no
// longer go back to that week; only an administrator can unlock it, with a reason on the audit trail.
@Service
@Transactional
@RequiredArgsConstructor
public class LockContinuousWeekUseCase {

    private final TeacherSubjectRepository teacherSubjectRepo;
    private final StudentAssessmentRepository studentAssessmentRepo;
    private final AssessmentScoreRepository assessmentScoreRepo;
    private final AssessmentCaEntryRepository caEntryRepo;
    private final ClassSubjectAssessmentLifeCycleRepository cycleRepo;
    private final AssessmentStructureService structureService;

    // What the cycle and its scores look like for one (teacher subject, assessment, term).
    record Loaded(com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle cycle, java.util.List<AssessmentScore> scores) {
    }

    Loaded load(String schoolId, String teacherSubjectId, String assessmentId, String termId) {
        var ts = teacherSubjectRepo.findByIdAndSchoolId(teacherSubjectId, schoolId)
                .orElseThrow(() -> new NotFoundException("Teacher subject not found"));
        var studentAssessments = studentAssessmentRepo.findAllBySubjectAndSessionAndTermId(ts.getSubject().getId(),
                ts.getSession().getId(), termId);
        var scores = new ArrayList<AssessmentScore>();
        for (var sa : studentAssessments) {
            scores.addAll(assessmentScoreRepo.findAllByStudentAssessmentIdAndAssessmentId(sa.getId(), assessmentId));
        }
        if (scores.isEmpty()) {
            throw new NotFoundException("Assessment not found for these students");
        }
        var cycles = scores.stream().map(AssessmentScore::getCycle).distinct().toList();
        cycleRepo.saveAll(structureService.freezeOpened(cycles));
        return new Loaded(cycles.get(0), scores);
    }

    @AuditLogAnnotation(action = "CONTINUOUS_ASSESSMENT_WEEK_LOCKED")
    public void execute(String schoolId, String teacherSubjectId, String assessmentId, String termId, int week) {
        var loaded = load(schoolId, teacherSubjectId, assessmentId, termId);
        var cycle = loaded.cycle();

        var withWeek = new HashSet<String>();
        caEntryRepo.findAllByScoreIds(loaded.scores().stream().map(AssessmentScore::getId).toList()).stream()
                .filter(e -> e.getEntryNumber() == week).forEach(e -> withWeek.add(e.getAssessmentScoreId()));
        long missing = loaded.scores().stream().filter(s -> !withWeek.contains(s.getId())).count();
        if (missing > 0) {
            throw new RuleException("Recording no. " + week + " can only be locked once every student has a mark for "
                    + "it - " + missing + " still missing");
        }

        cycle.lockWeek(week);
        cycleRepo.save(cycle);
    }
}
