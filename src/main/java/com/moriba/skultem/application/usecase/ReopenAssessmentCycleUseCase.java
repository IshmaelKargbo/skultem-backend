package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AssessmentCycleDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.AssessmentCycleMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Admin/owner override: reopens one assessment cycle (a class's subject+term+assessment component -
 * e.g. "JSS1A Math Term 1 CA1") for editing, regardless of how far it's progressed
 * (SUBMITTED/APPROVED/COMPLETED/LOCKED). A teacher can only edit a score while its cycle is
 * DRAFT/RETURNED ({@link ClassSubjectAssessmentLifeCycle#canEdit}); this is the escape hatch for
 * fixing a mistake found after that window closed - e.g. a grade already approved or locked.
 * <p>
 * Only works while the cycle's academic year is still open. Once a year is closed, its grades are
 * frozen for good - this deliberately does not offer a way around that, even for admins.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ReopenAssessmentCycleUseCase {

    private final ClassSubjectAssessmentLifeCycleRepository cycleRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "ASSESSMENT_CYCLE_REOPENED")
    public AssessmentCycleDTO execute(String schoolId, String teacherSubjectId, String assessmentId, String termId,
            String note) {
        var cycle = cycleRepo.findByTeacherSubjectAndAssessmentAndTerm(teacherSubjectId, assessmentId, termId)
                .orElseThrow(() -> new NotFoundException("Assessment cycle not found"));

        var academicYear = cycle.getTerm().getAcademicYear();
        if (academicYear.isLocked()) {
            throw new RuleException(
                    "Cannot reopen this assessment - " + academicYear.getName() + " has been closed");
        }

        if (cycle.canEdit()) {
            throw new RuleException("This assessment is already open for editing");
        }

        ClassSubjectAssessmentLifeCycle.Status previousStatus = cycle.getStatus();
        cycle.markDraft();
        cycleRepo.save(cycle);

        logActivityUseCase.log(
                schoolId,
                ActivityType.SUBJECT,
                "Assessment reopened for editing",
                cycle.getSubject().getSession().getName() + " - " + cycle.getAssessment().getName() + " - "
                        + cycle.getTerm().getName() + " (was " + previousStatus + ")"
                        + (note == null || note.isBlank() ? "" : ": " + note),
                null,
                cycle.getId());

        return AssessmentCycleMapper.toDTO(cycle);
    }
}
