package com.moriba.skultem.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.AssessmentApprovalRequest;
import com.moriba.skultem.domain.repository.AssessmentApprovalRequestRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.ClassMasterRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;
import com.moriba.skultem.domain.repository.StudentAssessmentRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SubmitAssessmentForApprovalUseCase {

        private final AssessmentApprovalRequestRepository approvalRepo;
        private final TeacherSubjectRepository teacherSubjectRepo;
        private final AssessmentScoreRepository assessmentScoreRepo;
        private final StudentAssessmentRepository studentAssessmentRepo;
        private final ClassSubjectAssessmentLifeCycleRepository assessmentLifeCycleRepo;
        private final ClassMasterRepository classMasterRepo;

        @AuditLogAnnotation(action = "ASSESSMENT_SUBMITED")
        public void execute(
                        String schoolId,
                        String teacherSubjectId,
                        String assessmentId,
                        String termId,
                        String note) {

                var teacherSubject = teacherSubjectRepo
                                .findByIdAndSchoolId(teacherSubjectId, schoolId)
                                .orElseThrow(() -> new NotFoundException("Teacher subject not found"));

                // A session can have more than one active class master - any one of them can review
                // and approve, so route to whichever was assigned most recently rather than requiring
                // exactly one to exist.
                var classMaster = classMasterRepo
                                .findTopByClassSessionIdAndEndedAtIsNullOrderByAssignedAtDesc(
                                                teacherSubject.getSession().getId())
                                .orElseThrow(() -> new NotFoundException("Class master not found"));

                // Resolved by subject + session, not the specific teacherSubjectId - a subject can
                // have more than one teacher assigned, sharing one cycle/gradebook rather than each
                // teacher's assignment spawning its own.
                var cycle = assessmentLifeCycleRepo
                                .findBySubjectSessionAssessmentAndTerm(
                                                teacherSubject.getSubject().getId(),
                                                teacherSubject.getSession().getId(),
                                                assessmentId,
                                                termId)
                                .orElseThrow(() -> new NotFoundException("Assessment cycle not found"));

                var studentAssessments = studentAssessmentRepo
                                .findAllBySubjectAndSessionAndTermId(teacherSubject.getSubject().getId(),
                                                teacherSubject.getSession().getId(), termId);

                if (studentAssessments.isEmpty()) {
                        throw new NotFoundException("No student assessments found");
                }

                for (var sa : studentAssessments) {

                        var scores = assessmentScoreRepo
                                        .findAllByStudentAssessmentIdAndAssessmentId(sa.getId(), assessmentId);

                        if (scores.isEmpty()) {
                                throw new RuleException("Assessment scores are missing");
                        }

                        scores.forEach(score -> {
                                if (!score.getCycle().canEdit()) {
                                        throw new RuleException(
                                                        "Assessment cannot be edited in the current state");
                                }
                        });
                }

                // By cycle alone - a cycle has at most one live approval request regardless of which
                // of the subject's teachers submits it, so two co-teachers can't each spawn their own.
                var assessmentRes = approvalRepo.findByCycle(cycle.getId());
                AssessmentApprovalRequest approvalRequest;

                if (assessmentRes.isEmpty()) {
                        approvalRequest = AssessmentApprovalRequest.create(
                                        UUID.randomUUID().toString(),
                                        schoolId,
                                        classMaster,
                                        cycle,
                                        teacherSubject,
                                        cycle.getTerm(),
                                        note);
                } else {
                        approvalRequest = assessmentRes.get();
                        approvalRequest.resubmit(note);
                }

                cycle.submit();
                approvalRepo.save(approvalRequest);
                assessmentLifeCycleRepo.save(cycle);
        }
}
