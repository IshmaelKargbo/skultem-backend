package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.AssessmentApprovalRequestRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ReturnAssessmentUseCase {

    private final AssessmentApprovalRequestRepository approvalRepo;
    private final ClassSubjectAssessmentLifeCycleRepository cycleRepo;

    @AuditLogAnnotation(action = "ASSESSMENT_APPROVAL_RETURNED")
    public void execute(String schoolId, String approvalRequestId, String note) {

        var approvalRequest = approvalRepo
                .findByIdAndSchoolId(approvalRequestId, schoolId)
                .orElseThrow(() -> new NotFoundException("Approval request not found"));

        if (!approvalRequest.isPending()) {
            throw new RuleException("Approval request is not pending");
        }

        var teacherSubjectId = approvalRequest.getTeacherSubject().getId();
        var assessmentId = approvalRequest.getCycle().getAssessment().getId();
        var termId = approvalRequest.getTerm().getId();
        
        var cycle = cycleRepo
                .findByTeacherSubjectAndAssessmentAndTerm(
                        teacherSubjectId,
                        assessmentId,
                        termId)
                .orElseThrow(() -> new NotFoundException("Assessment cycle not found"));

        cycle.returnForCorrection();
        cycleRepo.save(cycle);
        approvalRequest.returnRequest(note);
        approvalRepo.save(approvalRequest);
    }
}
