package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// The only way back into a locked CA recording: an administrator (owner, proprietor or admin) unlocks it, and has to
// say why - the reason is kept on the audit trail. If the CA had already been submitted it is re-opened too and has
// to be submitted again after the correction.
@Service
@Transactional
@RequiredArgsConstructor
public class UnlockContinuousWeekUseCase {

    private final LockContinuousWeekUseCase loader;
    private final ClassSubjectAssessmentLifeCycleRepository cycleRepo;

    @AuditLogAnnotation(action = "CONTINUOUS_ASSESSMENT_WEEK_UNLOCKED")
    public void execute(String schoolId, String teacherSubjectId, String assessmentId, String termId, int week,
            String reason) {
        if (reason == null || reason.isBlank()) {
            throw new RuleException("Say why recording no. " + week + " is being unlocked");
        }
        var cycle = loader.load(schoolId, teacherSubjectId, assessmentId, termId).cycle();
        cycle.unlockWeek(week);
        cycleRepo.save(cycle);
    }
}
