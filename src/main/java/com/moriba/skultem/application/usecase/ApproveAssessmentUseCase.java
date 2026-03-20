package com.moriba.skultem.application.usecase;

import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.events.GradesReleasedEvent;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.repository.AssessmentApprovalRequestRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ApproveAssessmentUseCase {

    private final AssessmentApprovalRequestRepository approvalRepo;
    private final AssessmentScoreRepository assessmentRepo;
    private final ClassSubjectAssessmentLifeCycleRepository cycleRepo;
    private final ApplicationEventPublisher eventPublisher;

    @AuditLogAnnotation(action = "ASSESSMENT_APPROVED")
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

        cycle.approve();
        cycleRepo.save(cycle);
        approvalRequest.approve(note);
        approvalRepo.save(approvalRequest);

        List<AssessmentScore> scores = assessmentRepo.findAllByCycle(cycle.getId());

        for (AssessmentScore score : scores) {
            var student = score.getStudentAssessment().getEnrollment().getStudent();
            var subject = score.getStudentAssessment().getTeacherSubject().getSubject();
            var teacher = score.getStudentAssessment().getTeacherSubject().getTeacher();
            var clazz = score.getStudentAssessment().getEnrollment().getClazz();
            var user = student.getParent().getUser();

            Map<String, String> meta = Map.of(
                    "student_id", student.getId(),
                    "student_name", student.getName(),
                    "score", score.getScore().toString(),
                    "weight", score.getWeight().toString(),
                    "weightScore", score.getWeightedScore().toString(),
                    "subject", subject.getName(),
                    "teacher", teacher.getName());

            eventPublisher.publishEvent(new GradesReleasedEvent(schoolId, user, student.getName(), subject.getName(),
                    score.getAssessment().getName(), cycle.getTerm().getName(), clazz.getName(),
                    score.getScore().intValue(), meta));
        }
    }
}
